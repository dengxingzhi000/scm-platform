package com.cognora.scm.finance;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 账期重开迁移（人工执行，未接入 Flyway）的文本约束：
 * 幂等、历史不回填、异常只报错不改写、状态约束覆盖 2-已重开。
 */
class FinanceBatch4MigrationSqlTest {

    private static final Path MIGRATION =
            Path.of("../../../scripts/db/migrations/finance_004_period_reopen.sql");

    @Test
    void migrationMustBeIdempotentAndReportOnly() throws Exception {
        assertTrue(Files.exists(MIGRATION), "缺少账期重开迁移: " + MIGRATION.toAbsolutePath());
        String sql = Files.readString(MIGRATION).toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("if not exists"), "DDL 必须使用 IF NOT EXISTS 保证幂等");
        assertTrue(sql.contains("pg_constraint"), "状态约束必须按 pg_constraint 判断后替换");
        assertTrue(sql.contains("raise exception"), "异常数据必须 RAISE EXCEPTION 中止");
        assertTrue(sql.contains("status in (0, 1, 2)"), "状态约束必须覆盖 2-已重开");
        assertTrue(sql.contains("reopened_by"), "缺少 reopened_by 列");
        assertTrue(sql.contains("reopened_at"), "缺少 reopened_at 列");
        assertTrue(sql.contains("reopen_reason"), "缺少 reopen_reason 列");
        assertFalse(sql.matches("(?s).*\\bupdate\\b.*"), "迁移不得改写既有数据");
        assertFalse(sql.matches("(?s).*\\bdelete\\s+from\\b.*"), "迁移不得删除既有数据");
    }
}
