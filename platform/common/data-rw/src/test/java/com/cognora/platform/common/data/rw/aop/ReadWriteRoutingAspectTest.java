package com.cognora.platform.common.data.rw.aop;

import com.cognora.platform.common.data.rw.annotation.Master;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.platform.common.data.rw.routing.ReadWriteRoutingContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("读写路由切面测试")
class ReadWriteRoutingAspectTest {

    private final ReadWriteRoutingAspect aspect = new ReadWriteRoutingAspect();

    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private MethodSignature signature;

    private final MasterTarget masterTarget = new MasterTarget();
    private final SlaveTarget slaveTarget = new SlaveTarget();
    private final TxTarget txTarget = new TxTarget();

    @BeforeEach
    void setUp() {
        ReadWriteRoutingContext.clear();
    }

    @AfterEach
    void tearDown() {
        ReadWriteRoutingContext.clear();
    }

    private void bind(Object target, String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(target);
    }

    @Test
    @DisplayName("@Master 成功返回后应标记写操作，保证后续读走主库")
    void masterShouldMarkWriteOnSuccess() throws Throwable {
        bind(masterTarget, "write");
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.aroundMaster(joinPoint);

        assertEquals("ok", result);
        assertNotNull(ReadWriteRoutingContext.getLastWriteTime(),
                "@Master 方法必须调用 markWrite()，否则写后读窗口失效");
        assertPoppedBackToAuto();
    }

    @Test
    @DisplayName("@Master 抛异常时不应标记写操作")
    void masterShouldNotMarkWriteOnFailure() throws Throwable {
        bind(masterTarget, "write");
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> aspect.aroundMaster(joinPoint));
        assertNull(ReadWriteRoutingContext.getLastWriteTime());
        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current(),
                "异常后必须弹栈，不能把 MASTER 泄漏给后续调用");
    }

    @Test
    @DisplayName("@Slave 应压入 SLAVE 并在返回后弹栈")
    void slaveShouldPushAndPop() throws Throwable {
        bind(slaveTarget, "read");
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.aroundSlave(joinPoint);

        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current());
        assertNull(ReadWriteRoutingContext.getLastWriteTime(), "只读方法不应标记写");
    }

    @Test
    @DisplayName("@Slave(value) 应把从库名带进当前作用域")
    void slaveShouldPropagateSpecifiedSlave() throws Throwable {
        bind(slaveTarget, "readFrom");
        String[] observed = new String[1];
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            observed[0] = ReadWriteRoutingContext.getSpecifiedSlave();
            return "ok";
        });

        aspect.aroundSlave(joinPoint);

        assertEquals("slave2", observed[0]);
        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current());
    }

    @Test
    @DisplayName("嵌套 @Slave 返回后应恢复外层从库选择")
    void nestedSlaveShouldRestoreOuterScope() throws Throwable {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, "slave1", true);

        bind(slaveTarget, "readFrom");
        when(joinPoint.proceed()).thenReturn("ok");
        aspect.aroundSlave(joinPoint);

        assertEquals("slave1", ReadWriteRoutingContext.getSpecifiedSlave());
        ReadWriteRoutingContext.pop();
    }

    @Test
    @DisplayName("fallbackToMaster=false 应传入上下文")
    void slaveWithoutFallbackShouldReachContext() throws Throwable {
        bind(slaveTarget, "readNoFallback");
        boolean[] observed = new boolean[1];
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            observed[0] = ReadWriteRoutingContext.isFallbackToMaster();
            return "ok";
        });

        aspect.aroundSlave(joinPoint);

        assertFalse(observed[0]);
        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current());
        assertTrue(ReadWriteRoutingContext.isFallbackToMaster());
    }

    private void bindMethodOnly(Object target, String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getName()).thenReturn(methodName);
        when(signature.getDeclaringType()).thenReturn(target.getClass());
    }

    @Test
    @DisplayName("readOnly 事务应路由到从库")
    void readOnlyTransactionShouldRouteToSlave() throws Throwable {
        bindMethodOnly(txTarget, "readOnlyTx");
        ReadWriteRoutingContext.RoutingType[] observed = new ReadWriteRoutingContext.RoutingType[1];
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            observed[0] = ReadWriteRoutingContext.current();
            return "ok";
        });

        aspect.aroundTransactional(joinPoint);

        assertEquals(ReadWriteRoutingContext.RoutingType.SLAVE, observed[0]);
        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current());
    }

    @Test
    @DisplayName("非 readOnly 事务应路由到主库并标记写")
    void writeTransactionShouldRouteToMaster() throws Throwable {
        bindMethodOnly(txTarget, "writeTx");
        ReadWriteRoutingContext.RoutingType[] observed = new ReadWriteRoutingContext.RoutingType[1];
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            observed[0] = ReadWriteRoutingContext.current();
            return "ok";
        });

        aspect.aroundTransactional(joinPoint);

        assertEquals(ReadWriteRoutingContext.RoutingType.MASTER, observed[0]);
        assertNotNull(ReadWriteRoutingContext.getLastWriteTime());
    }

    @Test
    @DisplayName("非 readOnly 事务嵌套在 @Slave 作用域内必须强制主库，不能把写事务留在从库上")
    void writeTransactionNestedInSlaveScopeShouldForceMaster() throws Throwable {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, "slave1", true);
        bindMethodOnly(txTarget, "writeTx");
        ReadWriteRoutingContext.RoutingType[] observed = new ReadWriteRoutingContext.RoutingType[1];
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            observed[0] = ReadWriteRoutingContext.current();
            return "ok";
        });

        aspect.aroundTransactional(joinPoint);

        assertEquals(ReadWriteRoutingContext.RoutingType.MASTER, observed[0]);
        assertEquals(ReadWriteRoutingContext.RoutingType.SLAVE, ReadWriteRoutingContext.current(),
                "事务结束后应恢复外层 @Slave 作用域");
    }

    private void assertPoppedBackToAuto() {
        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current(),
                "@Master 返回后必须弹栈");
    }

    @SuppressWarnings("unused")
    static class MasterTarget {
        @Master(reason = "test")
        public String write() {
            return "ok";
        }
    }

    @SuppressWarnings("unused")
    static class SlaveTarget {
        @Slave
        public String read() {
            return "ok";
        }

        @Slave("slave2")
        public String readFrom() {
            return "ok";
        }

        @Slave(value = "slave3", fallbackToMaster = false)
        public String readNoFallback() {
            return "ok";
        }
    }

    @SuppressWarnings("unused")
    static class TxTarget {
        @Transactional(readOnly = true)
        public String readOnlyTx() {
            return "ok";
        }

        @Transactional
        public String writeTx() {
            return "ok";
        }
    }
}