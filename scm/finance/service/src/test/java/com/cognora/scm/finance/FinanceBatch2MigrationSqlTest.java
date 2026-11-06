package com.cognora.scm.finance;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 财务迁移是人工执行的（未接入 Flyway），因此用文本断言守住两条硬约束：
 * 必须幂等、异常数据只报错不改写。
 */
class FinanceBatch2MigrationSqlTest {

    private static final Path MIGRATION =
            Path.of("../../../scripts/db/migrations/finance_002_batch2.sql");
    private static final Path SEED =
            Path.of("../../../scripts/db/seed/023_finance_batch2_permissions.sql");

    @Test
    void migrationMustBeIdempotentAndReportOnly() throws Exception {
        assertTrue(Files.exists(MIGRATION), "缺少财务批次 2 迁移: " + MIGRATION.toAbsolutePath());
        String sql = Files.readString(MIGRATION).toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("if not exists"), "DDL 必须使用 IF NOT EXISTS 保证幂等");
        assertTrue(sql.contains("pg_constraint"), "状态约束必须按 pg_constraint 判断后替换");
        assertTrue(sql.contains("raise exception"), "异常数据必须 RAISE EXCEPTION 中止");
        assertFalse(sql.matches("(?s).*\\bupdate\\b.*"), "迁移不得改写既有数据");
        assertFalse(sql.matches("(?s).*\\bdelete\\s+from\\b.*"), "迁移不得删除既有数据");
    }

    @Test
    void permissionSeedMustBeIdempotent() throws Exception {
        assertTrue(Files.exists(SEED), "缺少财务批次 2 权限种子: " + SEED.toAbsolutePath());
        String sql = Files.readString(SEED).toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("on conflict"), "权限种子必须使用 ON CONFLICT 保证幂等");
        assertTrue(sql.contains("finance:accounting-period"), "缺少账期权限");
        assertTrue(sql.contains("finance:reconciliation-diff"), "缺少对账差异权限");
        assertTrue(sql.contains("finance:fund-flow:create"), "缺少资金流水权限");
    }
}
