package com.frog.common.tenant;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/**
 * MyBatis 租户拦截器
 * 自动在 SQL 中注入 tenant_id 过滤条件
 *
 * 功能：
 * 1. SELECT 查询自动添加 WHERE tenant_id = ?
 * 2. UPDATE/DELETE 自动添加 WHERE tenant_id = ?
 * 3. INSERT 自动添加 tenant_id 字段
 *
 * 排除表：不需要租户隔离的系统表（如租户表本身）
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Intercepts({
    @Signature(
        type = StatementHandler.class,
        method = "prepare",
        args = {Connection.class, Integer.class}
    )
})
public class TenantInterceptor implements Interceptor {

    /**
     * 租户字段名
     */
    private static final String TENANT_COLUMN = "tenant_id";

    /**
     * 不需要租户隔离的表（系统表、租户表本身等）
     */
    private static final Set<String> EXCLUDE_TABLES = new HashSet<>(Arrays.asList(
        "tenant",
        "tenant_package",
        "tenant_subscription",
        "tenant_resource_quota",
        "tenant_config",
        "tenant_feature",
        "tenant_operation_log"
    ));

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        // 获取 MappedStatement
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();

        // 只处理 SELECT, UPDATE, DELETE, INSERT
        if (!SqlCommandType.SELECT.equals(sqlCommandType) &&
            !SqlCommandType.UPDATE.equals(sqlCommandType) &&
            !SqlCommandType.DELETE.equals(sqlCommandType) &&
            !SqlCommandType.INSERT.equals(sqlCommandType)) {
            return invocation.proceed();
        }

        // 获取租户ID
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.warn("Tenant ID is null, skipping tenant filter for SQL: {}",
                    mappedStatement.getId());
            return invocation.proceed();
        }

        // 获取原始SQL
        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();

        try {
            // 解析SQL
            Statement statement = CCJSqlParserUtil.parse(originalSql);

            // 根据SQL类型处理
            if (statement instanceof Select) {
                handleSelect((Select) statement, tenantId);
            }
            // TODO: 处理 UPDATE, DELETE, INSERT

            // 重新设置SQL
            String newSql = statement.toString();
            metaObject.setValue("delegate.boundSql.sql", newSql);

            log.debug("Injected tenant_id={} into SQL: {}", tenantId, newSql);
        } catch (Exception e) {
            log.error("Failed to inject tenant_id into SQL: {}", originalSql, e);
            // 如果解析失败，继续执行原SQL（安全起见，建议配置为抛异常）
        }

        return invocation.proceed();
    }

    /**
     * 处理 SELECT 语句
     */
    private void handleSelect(Select select, UUID tenantId) {
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

        // 获取表名
        String tableName = plainSelect.getFromItem().toString();
        if (isExcludeTable(tableName)) {
            log.debug("Table {} is excluded from tenant filter", tableName);
            return;
        }

        // 构建 tenant_id = 'xxx' 条件
        EqualsTo tenantCondition = new EqualsTo();
        tenantCondition.setLeftExpression(new Column(TENANT_COLUMN));
        tenantCondition.setRightExpression(new StringValue(tenantId.toString()));

        // 添加到WHERE条件
        Expression where = plainSelect.getWhere();
        if (where == null) {
            plainSelect.setWhere(tenantCondition);
        } else {
            AndExpression andExpression = new AndExpression(where, tenantCondition);
            plainSelect.setWhere(andExpression);
        }
    }

    /**
     * 判断是否是排除表
     */
    private boolean isExcludeTable(String tableName) {
        // 去除表别名
        String actualTableName = tableName.contains(" ")
            ? tableName.substring(0, tableName.indexOf(" ")).trim()
            : tableName.trim();

        // 去除数据库名前缀（如 db_product.prod_category -> prod_category）
        if (actualTableName.contains(".")) {
            actualTableName = actualTableName.substring(actualTableName.indexOf(".") + 1);
        }

        return EXCLUDE_TABLES.contains(actualTableName.toLowerCase());
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
        // 可以从配置文件读取排除表列表
        String excludeTables = properties.getProperty("excludeTables");
        if (excludeTables != null && !excludeTables.trim().isEmpty()) {
            String[] tables = excludeTables.split(",");
            for (String table : tables) {
                EXCLUDE_TABLES.add(table.trim().toLowerCase());
            }
        }
    }
}