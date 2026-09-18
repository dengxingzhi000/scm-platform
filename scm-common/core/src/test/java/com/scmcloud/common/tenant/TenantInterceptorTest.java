package com.scmcloud.common.tenant;

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
        assertThat(result).contains("WHERE tenant_id =");
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
}
