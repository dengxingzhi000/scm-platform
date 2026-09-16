package com.scmcloud.common.tenant;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
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
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
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
 * MyBatis 绉熸埛鎷︽埅锟?
 * 鑷姩锟絊QL 涓敞锟絫enant_id 杩囨护鏉′欢
 *
 * 鍔熻兘锟?
 * 1. SELECT 鏌ヨ鑷姩娣诲姞 WHERE tenant_id = ?
 * 2. UPDATE/DELETE 鑷姩娣诲姞 WHERE tenant_id = ?
 * 3. INSERT 鑷姩娣诲姞 tenant_id 瀛楁
 *
 * 鎺掗櫎琛細涓嶉渶瑕佺鎴烽殧绂荤殑绯荤粺琛紙濡傜鎴疯〃鏈韩锟?
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
     * 绉熸埛瀛楁锟?
     */
    private static final String TENANT_COLUMN = "tenant_id";

    /**
     * 涓嶉渶瑕佺鎴烽殧绂荤殑琛紙绯荤粺琛ㄣ€佺鎴疯〃鏈韩绛夛級
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

        // 鑾峰彇 MappedStatement
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();

        // 鍙锟絊ELECT, UPDATE, DELETE, INSERT
        if (!SqlCommandType.SELECT.equals(sqlCommandType) &&
            !SqlCommandType.UPDATE.equals(sqlCommandType) &&
            !SqlCommandType.DELETE.equals(sqlCommandType) &&
            !SqlCommandType.INSERT.equals(sqlCommandType)) {
            return invocation.proceed();
        }

        // 鑾峰彇绉熸埛ID
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.warn("Tenant ID is null, skipping tenant filter for SQL: {}",
                    mappedStatement.getId());
            return invocation.proceed();
        }

        // 鑾峰彇鍘熷SQL
        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();

        try {
            // 瑙f瀽SQL
            Statement statement = CCJSqlParserUtil.parse(originalSql);

            // 鏍规嵁SQL绫诲瀷澶勭悊
            if (statement instanceof Select select) {
                handleSelect(select, tenantId);
            } else if (statement instanceof Update update) {
                handleUpdate(update, tenantId);
            } else if (statement instanceof Delete delete) {
                handleDelete(delete, tenantId);
            } else if (statement instanceof Insert insert) {
                // INSERT 璇彞锟絫enant_id 鐢卞簲鐢ㄥ眰璁剧疆锛屼笉鍦ㄦ嫤鎴櫒涓锟?
                log.debug("INSERT statement detected, tenant_id should be set by application layer");
            }

            // 閲嶆柊璁剧疆SQL
            String newSql = statement.toString();
            metaObject.setValue("delegate.boundSql.sql", newSql);

            log.debug("Injected tenant_id={} into SQL: {}", tenantId, newSql);
        } catch (Exception e) {
            log.error("Failed to inject tenant_id into SQL: {}", originalSql, e);
            // 濡傛灉瑙f瀽澶辫触锛岀户缁墽琛屽師SQL锛堝畨鍏ㄨ捣瑙侊紝寤鸿閰嶇疆涓烘姏寮傚父锟?
        }

        return invocation.proceed();
    }

    /**
     * 澶勭悊 SELECT 璇彞
     */
    private void handleSelect(Select select, UUID tenantId) {
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

        // 鑾峰彇琛ㄥ悕
        String tableName = plainSelect.getFromItem().toString();
        if (isExcludeTable(tableName)) {
            log.debug("Table {} is excluded from tenant filter", tableName);
            return;
        }

        // 鏋勫缓 tenant_id = 'xxx' 鏉′欢
        EqualsTo tenantCondition = buildTenantCondition(tenantId);

        // 娣诲姞鍒癢HERE鏉′欢
        Expression where = plainSelect.getWhere();
        if (where == null) {
            plainSelect.setWhere(tenantCondition);
        } else {
            AndExpression andExpression = new AndExpression(where, tenantCondition);
            plainSelect.setWhere(andExpression);
        }
    }

    /**
     * 澶勭悊 UPDATE 璇彞 - 娣诲姞 tenant_id 锟絎HERE 鏉′欢
     */
    private void handleUpdate(Update update, UUID tenantId) {
        Table table = update.getTable();
        if (table != null && isExcludeTable(table.toString())) {
            log.debug("Table {} is excluded from tenant filter", table);
            return;
        }

        EqualsTo tenantCondition = buildTenantCondition(tenantId);

        Expression where = update.getWhere();
        if (where == null) {
            update.setWhere(tenantCondition);
        } else {
            AndExpression andExpression = new AndExpression(where, tenantCondition);
            update.setWhere(andExpression);
        }
    }

    /**
     * 澶勭悊 DELETE 璇彞 - 娣诲姞 tenant_id 锟絎HERE 鏉′欢
     */
    private void handleDelete(Delete delete, UUID tenantId) {
        Table table = delete.getTable();
        if (table != null && isExcludeTable(table.toString())) {
            log.debug("Table {} is excluded from tenant filter", table);
            return;
        }

        EqualsTo tenantCondition = buildTenantCondition(tenantId);

        Expression where = delete.getWhere();
        if (where == null) {
            delete.setWhere(tenantCondition);
        } else {
            AndExpression andExpression = new AndExpression(where, tenantCondition);
            delete.setWhere(andExpression);
        }
    }

    /**
     * 鏋勫缓 tenant_id = 'xxx' 鏉′欢琛ㄨ揪锟?
     */
    private EqualsTo buildTenantCondition(UUID tenantId) {
        EqualsTo condition = new EqualsTo();
        condition.setLeftExpression(new Column(TENANT_COLUMN));
        condition.setRightExpression(new StringValue(tenantId.toString()));
        return condition;
    }

    /**
     * 鍒ゆ柇鏄惁鏄帓闄よ〃
     */
    private boolean isExcludeTable(String tableName) {
        // 鍘婚櫎琛ㄥ埆锟?
        String actualTableName = tableName.contains(" ")
            ? tableName.substring(0, tableName.indexOf(" ")).trim()
            : tableName.trim();

        // 鍘婚櫎鏁版嵁搴撳悕鍓嶇紑锛堝 db_product.prod_category -> prod_category锟?
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
        // 鍙互浠庨厤缃枃浠惰鍙栨帓闄よ〃鍒楄〃
        String excludeTables = properties.getProperty("excludeTables");
        if (excludeTables != null && !excludeTables.trim().isEmpty()) {
            String[] tables = excludeTables.split(",");
            for (String table : tables) {
                EXCLUDE_TABLES.add(table.trim().toLowerCase());
            }
        }
    }
}