package com.scmcloud.common.tenant;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Intercepts({
        @Signature(
                type = StatementHandler.class,
                method = "prepare",
                args = {Connection.class, Integer.class}
        )
})
public class TenantInterceptor implements Interceptor {

    private static final String TENANT_COLUMN = "tenant_id";

    private static final Set<String> EXCLUDE_TABLES = new HashSet<>(Arrays.asList(
            "tenant", "tenant_package", "tenant_subscription",
            "tenant_resource_quota", "tenant_config", "tenant_feature", "tenant_operation_log"
    ));

    private final TenantProperties properties;

    private final Cache<String, String> rewrittenSqlCache = Caffeine.newBuilder()
            .maximumSize(1024)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    public TenantInterceptor(TenantProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();

        if (!SqlCommandType.SELECT.equals(sqlCommandType)
                && !SqlCommandType.UPDATE.equals(sqlCommandType)
                && !SqlCommandType.DELETE.equals(sqlCommandType)
                && !SqlCommandType.INSERT.equals(sqlCommandType)) {
            return invocation.proceed();
        }

        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.debug("Tenant ID is null, skipping tenant filter for SQL: {}", mappedStatement.getId());
            return invocation.proceed();
        }

        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();
        String cacheKey = mappedStatement.getId() + "::" + tenantId + "::" + originalSql;
        String cached = rewrittenSqlCache.getIfPresent(cacheKey);
        final String newSql;
        if (cached != null) {
            newSql = cached;
        } else {
            newSql = rewriteSql(originalSql, tenantId);
            rewrittenSqlCache.put(cacheKey, newSql);
        }
        metaObject.setValue("delegate.boundSql.sql", newSql);

        return invocation.proceed();
    }

    /**
     * Rewrite SQL to inject tenant_id condition. Package-private for testing.
     *
     * @throws TenantParseException when failOnParseError is true and SQL is unparseable
     */
    String rewriteSql(String originalSql, UUID tenantId) {
        try {
            Statement statement = CCJSqlParserUtil.parse(originalSql);

            if (statement instanceof Select select) {
                handleSelect(select, tenantId);
            } else if (statement instanceof Update update) {
                handleUpdate(update, tenantId);
            } else if (statement instanceof Delete delete) {
                handleDelete(delete, tenantId);
            }
            // INSERT handled by AuditMetaObjectHandler, no rewrite here
            return statement.toString();
        } catch (Exception e) {
            if (properties != null && properties.failOnParseError()) {
                throw new TenantParseException(
                        "Failed to inject tenant_id into SQL: " + originalSql, e);
            }
            log.warn("Failed to inject tenant_id into SQL (skipping): {}", originalSql, e);
            return originalSql;
        }
    }

    private void handleSelect(Select select, UUID tenantId) {
        if (select.getSelectBody() instanceof SetOperationList setOp) {
            for (Object body : setOp.getSelects()) {
                if (body instanceof PlainSelect ps) {
                    injectIntoPlainSelect(ps, tenantId);
                }
            }
        } else if (select.getSelectBody() instanceof PlainSelect plainSelect) {
            injectIntoPlainSelect(plainSelect, tenantId);
        }
    }

    private void injectIntoPlainSelect(PlainSelect plainSelect, UUID tenantId) {
        String tableName = plainSelect.getFromItem().toString();
        if (isExcludeTable(tableName)) {
            log.debug("Table {} is excluded from tenant filter", tableName);
            return;
        }
        EqualsTo condition = buildTenantCondition(tenantId);
        Expression where = plainSelect.getWhere();
        if (where == null) {
            plainSelect.setWhere(condition);
        } else {
            plainSelect.setWhere(new AndExpression(where, condition));
        }
    }

    private void handleUpdate(Update update, UUID tenantId) {
        Table table = update.getTable();
        if (table != null && isExcludeTable(table.toString())) {
            return;
        }
        EqualsTo condition = buildTenantCondition(tenantId);
        Expression where = update.getWhere();
        if (where == null) {
            update.setWhere(condition);
        } else {
            update.setWhere(new AndExpression(where, condition));
        }
    }

    private void handleDelete(Delete delete, UUID tenantId) {
        Table table = delete.getTable();
        if (table != null && isExcludeTable(table.toString())) {
            return;
        }
        EqualsTo condition = buildTenantCondition(tenantId);
        Expression where = delete.getWhere();
        if (where == null) {
            delete.setWhere(condition);
        } else {
            delete.setWhere(new AndExpression(where, condition));
        }
    }

    private EqualsTo buildTenantCondition(UUID tenantId) {
        EqualsTo condition = new EqualsTo();
        condition.setLeftExpression(new Column(TENANT_COLUMN));
        condition.setRightExpression(new StringValue(tenantId.toString()));
        return condition;
    }

    private boolean isExcludeTable(String tableName) {
        String actual = tableName.contains(" ")
                ? tableName.substring(0, tableName.indexOf(" ")).trim()
                : tableName.trim();
        if (actual.contains(".")) {
            actual = actual.substring(actual.indexOf(".") + 1);
        }
        return EXCLUDE_TABLES.contains(actual.toLowerCase());
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {
        String excludeTables = properties.getProperty("excludeTables");
        if (excludeTables != null && !excludeTables.trim().isEmpty()) {
            for (String t : excludeTables.split(",")) {
                EXCLUDE_TABLES.add(t.trim().toLowerCase());
            }
        }
    }
}
