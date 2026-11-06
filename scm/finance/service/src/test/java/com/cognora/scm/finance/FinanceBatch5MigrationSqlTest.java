package com.cognora.scm.finance;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 税率规则迁移（人工执行，未接入 Flyway）的文本约束：
 * 幂等、新表、历史结算单税率不回填。
 */
class FinanceBatch5MigrationSqlTest {

    private static final Path MIGRATION =
            Path.of("../../../scripts/db/migrations/finance_005_tax_rate.sql");

    @Test
    void migrationMustBeIdempotentAndLeaveHistoryUntouched() throws Exception {
        assertTrue(Files.exists(MIGRATION), "缺少税率规则迁移: " + MIGRATION.toAbsolutePath());
        String sql = Files.readString(MIGRATION).toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("if not exists"), "DDL 必须使用 IF NOT EXISTS 保证幂等");
        assertTrue(sql.contains("create table if not exists fin_tax_rate"), "缺少 fin_tax_rate 表");
        assertTrue(sql.contains("add column if not exists tax_rate"), "缺少 settlement_order.tax_rate 列");
        assertTrue(sql.contains("uk_tax_rate_tenant_default"), "缺少租户默认税率唯一索引");
        assertTrue(sql.contains("chk_tax_rate_value"), "缺少税率 0..1 约束");
        assertFalse(sql.matches("(?s).*\\bupdate\\b.*"), "迁移不得改写既有数据");
        assertFalse(sql.matches("(?s).*\\bdelete\\s+from\\b.*"), "迁移不得删除既有数据");
    }
}
