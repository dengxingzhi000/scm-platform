package com.scmcloud.common.data.rw.sql;

import com.scmcloud.common.data.rw.routing.ReadWriteRoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * MyBatis SQL routing interceptor.
 * <p>
 * Parses SQL type before execution and sets routing context.
 * <p>
 * It only makes a decision when no explicit scope is active. A SELECT is sent to a
 * slave only on a servlet request thread; async, scheduled and startup threads keep
 * the master default so they never silently observe stale data.
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class SqlRoutingInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // Skip if explicit routing is already set
        if (ReadWriteRoutingContext.current() != ReadWriteRoutingContext.RoutingType.AUTO) {
            return invocation.proceed();
        }

        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];

        BoundSql boundSql = ms.getBoundSql(parameter);
        String sql = boundSql.getSql();

        // 1. Parse hint
        SqlTypeParser.RoutingHint hint = SqlTypeParser.parseHint(sql);
        if (hint.type() != SqlTypeParser.RoutingHint.HintType.NONE) {
            return executeWithHint(invocation, hint);
        }

        // 2. Route by MyBatis SqlCommandType
        SqlCommandType commandType = ms.getSqlCommandType();
        if (commandType == SqlCommandType.SELECT) {
            // Check for FOR UPDATE
            SqlTypeParser.SqlType sqlType = SqlTypeParser.parse(sql);
            if (sqlType == SqlTypeParser.SqlType.WRITE) {
                log.debug("[SQL-Routing] Detected SELECT FOR UPDATE, routing to MASTER");
                return executeWithMaster(invocation);
            }

            if (!ReadWriteRoutingContext.isRequestScope()) {
                // No request scope: keep the datasource default (master). Prevents
                // async / scheduled reads from landing on a replica by accident.
                return invocation.proceed();
            }

            log.debug("[SQL-Routing] Detected SELECT in request scope, routing to SLAVE");
            return executeWithSlave(invocation);
        }

        // INSERT/UPDATE/DELETE -> master and mark write
        log.debug("[SQL-Routing] Detected {} operation, routing to MASTER", commandType);
        return executeWithMasterAndMarkWrite(invocation);
    }

    private Object executeWithHint(Invocation invocation, SqlTypeParser.RoutingHint hint)
            throws Throwable {
        switch (hint.type()) {
            case MASTER -> {
                log.debug("[SQL-Routing] Hint: MASTER");
                return executeWithMaster(invocation);
            }
            case SLAVE -> {
                log.debug("[SQL-Routing] Hint: SLAVE({})", hint.slaveName());
                return executeWithSlave(invocation, hint.slaveName());
            }
            default -> {
                return invocation.proceed();
            }
        }
    }

    private Object executeWithMaster(Invocation invocation) throws Throwable {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER, null, true);
        try {
            return invocation.proceed();
        } finally {
            ReadWriteRoutingContext.pop();
        }
    }

    private Object executeWithMasterAndMarkWrite(Invocation invocation) throws Throwable {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER, null, true);
        try {
            Object result = invocation.proceed();
            ReadWriteRoutingContext.markWrite();
            return result;
        } finally {
            ReadWriteRoutingContext.pop();
        }
    }

    private Object executeWithSlave(Invocation invocation) throws Throwable {
        return executeWithSlave(invocation, null);
    }

    private Object executeWithSlave(Invocation invocation, String slaveName) throws Throwable {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, slaveName, true);
        try {
            return invocation.proceed();
        } finally {
            ReadWriteRoutingContext.pop();
        }
    }
}
