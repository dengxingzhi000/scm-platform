package com.cognora.platform.audit;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审计迁移（人工执行）的文本约束：幂等、只新增索引/列，不改写历史数据。
 */
class AuditTimeRangeMigrationSqlTest {

    private static final Path MIGRATION =
            Path.of("../../../scripts/db/migrations/audit_003_audit_log_time_range_index.sql");

    private static final Path NONCE_MIGRATION =
            Path.of("../../../scripts/db/migrations/audit_004_audit_log_request_nonce.sql");

    @Test
    void migrationMustBeIdempotentAndIndexOnly() throws Exception {
        assertTrue(Files.exists(MIGRATION), "缺少审计时间范围索引迁移: " + MIGRATION.toAbsolutePath());
        String sql = Files.readString(MIGRATION).toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("create index if not exists"), "索引必须使用 IF NOT EXISTS");
        assertTrue(sql.contains("(tenant_id, create_time desc)"), "索引必须覆盖租户与时间范围");
        assertFalse(sql.matches("(?s).*\\bupdate\\b.*"), "迁移不得改写既有数据");
        assertFalse(sql.matches("(?s).*\\bdelete\\s+from\\b.*"), "迁移不得删除既有数据");
    }

    @Test
    void nonceMigrationMustAddTheColumnWithoutRewritingHistory() throws Exception {
        assertTrue(Files.exists(NONCE_MIGRATION), "缺少审计写入幂等迁移: " + NONCE_MIGRATION.toAbsolutePath());
        String sql = Files.readString(NONCE_MIGRATION).toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("add column if not exists request_nonce"), "列必须使用 IF NOT EXISTS");
        assertTrue(sql.contains("create unique index if not exists"), "唯一索引必须使用 IF NOT EXISTS");
        // 分区表上的唯一索引必须带分区键，否则 PostgreSQL 建不出来。
        assertTrue(sql.contains("(tenant_id, request_nonce, create_time)"), "唯一索引必须包含分区键");
        assertTrue(sql.contains("where request_nonce is not null"), "历史空值行不进唯一索引");
        assertFalse(sql.matches("(?s).*\\bupdate\\b.*"), "迁移不得回填历史数据");
        assertFalse(sql.matches("(?s).*\\bdelete\\s+from\\b.*"), "迁移不得删除既有数据");
    }
}
