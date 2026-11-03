package com.cognora.platform.common.tenant;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SetOperationList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantInterceptorTest {

    private TenantInterceptor interceptor;
    private TenantProperties properties;
    private UUID tenantId;

    @BeforeEach
    void setup() {
        properties = new TenantProperties(true, true, List.of());
        interceptor = new TenantInterceptor(properties);
        tenantId = UUID.randomUUID();
    }

    @AfterEach
    void teardown() {
        TenantContextHolder.clear();
    }

    @Test
    void selectWithTenant_shouldInjectWhereClause() {
        var result = interceptor.rewriteSql("SELECT id FROM ord_order", tenantId);
        assertThat(result).contains("WHERE ord_order.tenant_id =");
    }

    @Test
    void dashboardAggregate_shouldKeepGroupingAndInjectTenant() {
        // Shape produced by OrdOrderDashboardQueryService: zone-explicit buckets and
        // ::timestamptz casts on the bound range boundaries.
        var daily = interceptor.rewriteSql(
                "SELECT DATE(create_time at time zone 'Asia/Shanghai') AS bucket, COUNT(*) AS order_count, "
                        + "COALESCE(SUM(CASE WHEN status IN (1,2,3,4,5,6) THEN payable_amount ELSE 0 END), 0) AS revenue "
                        + "FROM ord_order WHERE deleted = ? AND (create_time >= (?::timestamptz)) "
                        + "AND (create_time < (?::timestamptz)) "
                        + "GROUP BY DATE(create_time at time zone 'Asia/Shanghai') "
                        + "ORDER BY DATE(create_time at time zone 'Asia/Shanghai') ASC",
                tenantId);
        assertThat(daily).contains("tenant_id =");
        // JSqlParser normalises the operator to upper case.
        assertThat(daily).containsIgnoringCase("GROUP BY DATE(create_time at time zone 'Asia/Shanghai')");

        var hourly = interceptor.rewriteSql(
                "SELECT date_trunc('hour', create_time at time zone 'Asia/Shanghai') AS bucket, COUNT(*) AS order_count "
                        + "FROM ord_order WHERE deleted = ? "
                        + "GROUP BY date_trunc('hour', create_time at time zone 'Asia/Shanghai')",
                tenantId);
        assertThat(hourly).contains("tenant_id =");
        assertThat(hourly).containsIgnoringCase("date_trunc('hour', create_time at time zone 'Asia/Shanghai')");
    }

    @Test
    void updateWithTenant_shouldInjectWhereClause() {
        var result = interceptor.rewriteSql(
                "UPDATE ord_order SET status = 'PAID' WHERE id = 1", tenantId);
        assertThat(result).contains("tenant_id =");
        assertThat(result).contains("id = 1");
    }

    @Test
    void deleteWithTenant_shouldInjectWhereClause() {
        var result = interceptor.rewriteSql("DELETE FROM ord_order WHERE id = 1", tenantId);
        assertThat(result).contains("tenant_id =");
    }

    @Test
    void unionSelect_shouldInjectIntoEachBranch() throws Exception {
        var sql = "SELECT id FROM ord_order UNION SELECT id FROM ord_refund";
        var result = interceptor.rewriteSql(sql, tenantId);
        var parsed = (SetOperationList) CCJSqlParserUtil.parse(result);
        var first = (PlainSelect) parsed.getSelects().get(0);
        var second = (PlainSelect) parsed.getSelects().get(1);
        assertThat(first.getWhere()).isNotNull();
        assertThat(second.getWhere()).isNotNull();
    }

    @Test
    void excludeTable_shouldSkipInjection() {
        var result = interceptor.rewriteSql("SELECT id FROM tenant", tenantId);
        assertThat(result).doesNotContain("tenant_id =");
    }

    @Test
    void unparseableSql_shouldThrowWhenFailFastEnabled() {
        assertThatThrownBy(() -> interceptor.rewriteSql("THIS IS NOT SQL", tenantId))
                .isInstanceOf(TenantParseException.class);
    }

    @Test
    void unparseableSql_shouldReturnOriginalWhenFailFastDisabled() {
        properties = new TenantProperties(false, true, List.of());
        interceptor = new TenantInterceptor(properties);
        var result = interceptor.rewriteSql("THIS IS NOT SQL", tenantId);
        assertThat(result).isEqualTo("THIS IS NOT SQL");
    }

    @Test
    void sameSqlDifferentCalls_shouldReturnConsistentResult() {
        var sql1 = interceptor.rewriteSql("SELECT id FROM ord_order", tenantId);
        var sql2 = interceptor.rewriteSql("SELECT id FROM ord_order", tenantId);
        assertThat(sql1).isEqualTo(sql2);
    }

    @Test
    void aliasedSelect_shouldQualifyTenantColumnWithTheAlias() {
        var result = interceptor.rewriteSql(
                "SELECT o.id FROM ord_order o WHERE o.deleted = ?", tenantId);
        assertThat(result).contains("o.tenant_id =");
        // A bare column would be ambiguous the moment the statement gains a join.
        assertThat(result).doesNotContain(" WHERE tenant_id");
    }

    @Test
    void derivedTable_shouldBeLeftAloneInsteadOfProducingBrokenSql() {
        var result = interceptor.rewriteSql(
                "SELECT x.id FROM (SELECT id FROM ord_order) x", tenantId);
        assertThat(result).doesNotContain("x.tenant_id");
        assertThat(result).doesNotContain("tenant_id =");
    }

    @Test
    void insert_shouldNotBeRewrittenBecauseTheMetaObjectHandlerFillsTenantId() {
        var result = interceptor.rewriteSql(
                "INSERT INTO ord_order (id, order_no) VALUES (1, 'ORD-1')", tenantId);
        assertThat(result).doesNotContain("tenant_id");
    }

    @Test
    void selectWithoutFromItem_shouldNotFail() {
        // Health checks and sequence calls have no table at all; the previous implementation
        // dereferenced a null FROM item.
        assertThat(interceptor.rewriteSql("SELECT 1", tenantId)).doesNotContain("tenant_id");
        assertThat(interceptor.rewriteSql("SELECT now()", tenantId)).doesNotContain("tenant_id");
    }

    @Test
    void frameworkTables_shouldNeverCarryATenantPredicate() {
        for (String table : List.of(
                "undo_log", "flyway_schema_history", "act_re_model", "flw_channel_definition",
                "qrtz_triggers", "xxl_job_info")) {
            assertThat(interceptor.rewriteSql("SELECT id FROM " + table, tenantId))
                    .as("table %s has no tenant_id column", table)
                    .doesNotContain("tenant_id =");
        }
    }

    @Test
    void auditTables_areTenantScopedNowThatTheColumnExists() {
        assertThat(interceptor.rewriteSql("SELECT id FROM sys_audit_log", tenantId))
                .contains("sys_audit_log.tenant_id =");
        assertThat(interceptor.rewriteSql("SELECT id FROM sys_sensitive_operation_log", tenantId))
                .contains("sys_sensitive_operation_log.tenant_id =");
    }

    @Test
    void configuredExcludeTables_areMergedWithTheBuiltInOnes() {
        interceptor = new TenantInterceptor(new TenantProperties(
                true, true, List.of(), List.of("cfg_shared_view", "report_*"), true));

        assertThat(interceptor.rewriteSql("SELECT id FROM cfg_shared_view", tenantId))
                .doesNotContain("tenant_id =");
        assertThat(interceptor.rewriteSql("SELECT id FROM report_daily_sales", tenantId))
                .doesNotContain("tenant_id =");
        assertThat(interceptor.rewriteSql("SELECT id FROM ord_order", tenantId))
                .contains("ord_order.tenant_id =");
    }
}
