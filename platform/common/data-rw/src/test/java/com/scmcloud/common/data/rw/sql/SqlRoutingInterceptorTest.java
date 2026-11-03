package com.scmcloud.common.data.rw.sql;

import com.scmcloud.common.data.rw.routing.ReadWriteRoutingContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MyBatis SQL 路由拦截器测试")
class SqlRoutingInterceptorTest {

    private final SqlRoutingInterceptor interceptor = new SqlRoutingInterceptor();

    @AfterEach
    void tearDown() {
        ReadWriteRoutingContext.clear();
    }

    private Invocation invocation(SqlCommandType commandType, String sql,
                                  AtomicReference<ReadWriteRoutingContext.RoutingType> observed)
            throws Exception {
        MappedStatement ms = mock(MappedStatement.class);
        when(ms.getSqlCommandType()).thenReturn(commandType);
        when(ms.getBoundSql(any())).thenReturn(
                new BoundSql(new Configuration(), sql, List.of(), null));

        Executor executor = (Executor) Proxy.newProxyInstance(
                Executor.class.getClassLoader(),
                new Class<?>[]{Executor.class},
                (proxy, method, args) -> {
                    if ("query".equals(method.getName())) {
                        observed.set(ReadWriteRoutingContext.current());
                        return List.of();
                    }
                    return null;
                });

        Method method = Executor.class.getMethod("query",
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class);
        return new Invocation(executor, method, new Object[]{ms, null, new RowBounds(), null});
    }

    private ReadWriteRoutingContext.RoutingType route(SqlCommandType type, String sql) throws Throwable {
        AtomicReference<ReadWriteRoutingContext.RoutingType> observed = new AtomicReference<>();
        interceptor.intercept(invocation(type, sql, observed));
        return observed.get();
    }

    @Test
    @DisplayName("请求线程内的未注解 SELECT 应走从库")
    void selectInRequestScopeShouldRouteToSlave() throws Throwable {
        ReadWriteRoutingContext.beginRequestScope();

        assertEquals(ReadWriteRoutingContext.RoutingType.SLAVE,
                route(SqlCommandType.SELECT, "SELECT * FROM prod_spu"));
        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current());
    }

    @Test
    @DisplayName("没有请求作用域（异步/定时任务）的 SELECT 保持主库默认")
    void selectWithoutRequestScopeShouldKeepDefault() throws Throwable {
        AtomicReference<ReadWriteRoutingContext.RoutingType> observed = new AtomicReference<>();
        interceptor.intercept(invocation(SqlCommandType.SELECT, "SELECT * FROM prod_spu", observed));

        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, observed.get(),
                "未进入请求作用域时应交给数据源的 default-target 决定（默认主库）");
    }

    @Test
    @DisplayName("写语句必须走主库并标记写")
    void insertShouldRouteToMasterAndMarkWrite() throws Throwable {
        ReadWriteRoutingContext.beginRequestScope();

        assertEquals(ReadWriteRoutingContext.RoutingType.MASTER,
                route(SqlCommandType.INSERT, "INSERT INTO prod_spu VALUES (?)"));
        assertNotNull(ReadWriteRoutingContext.getLastWriteTime());
    }

    @Test
    @DisplayName("SQL hint /* MASTER */ 在请求线程内也应强制主库")
    void masterHintShouldOverrideRequestScope() throws Throwable {
        ReadWriteRoutingContext.beginRequestScope();

        assertEquals(ReadWriteRoutingContext.RoutingType.MASTER,
                route(SqlCommandType.SELECT, "/* MASTER */ SELECT * FROM prod_spu"));
    }

    @Test
    @DisplayName("SELECT FOR UPDATE 必须走主库")
    void selectForUpdateShouldRouteToMaster() throws Throwable {
        ReadWriteRoutingContext.beginRequestScope();

        assertEquals(ReadWriteRoutingContext.RoutingType.MASTER,
                route(SqlCommandType.SELECT, "SELECT * FROM prod_spu FOR UPDATE"));
    }
}
