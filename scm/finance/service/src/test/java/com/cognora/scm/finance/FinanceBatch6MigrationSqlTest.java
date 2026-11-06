package com.cognora.scm.finance;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 坏账核销迁移（人工执行，未接入 Flyway）的文本约束：
 * 幂等、只建不改、重复核销由部分唯一索引兜底。
 */
class FinanceBatch6MigrationSqlTest {

    private static final Path MIGRATION =
            Path.of("../../../scripts/db/migrations/finance_006_bad_debt_write_off.sql");

    @Test
    void migrationMustBeIdempotentAndLeaveHistoryUntouched() throws Exception {
        assertTrue(Files.exists(MIGRATION), "缺少坏账核销迁移: " + MIGRATION.toAbsolutePath());
        String sql = Files.readString(MIGRATION).toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("if not exists"), "DDL 必须使用 IF NOT EXISTS 保证幂等");
        assertTrue(sql.contains("create table if not exists fin_bad_debt_write_off"),
                "缺少 fin_bad_debt_write_off 表");
        assertTrue(sql.contains("chk_bad_debt_amount_positive"), "缺少金额必须大于 0 的约束");
        assertTrue(sql.contains("chk_bad_debt_status"), "缺少状态 0..3 约束");
        assertTrue(sql.contains("uk_bad_debt_tenant_settlement"), "缺少同一结算单唯一索引");
        assertTrue(sql.contains("uk_bad_debt_tenant_invoice"), "缺少同一发票唯一索引");
        assertFalse(sql.matches("(?s).*\\bdelete\\s+from\\b.*"), "迁移不得删除既有数据");
    }
}
