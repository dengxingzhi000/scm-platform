package com.cognora.platform.audit;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审计分页时间范围索引迁移（人工执行）的文本约束：幂等、只新增索引。
 */
class AuditTimeRangeMigrationSqlTest {

    private static final Path MIGRATION =
            Path.of("../../../scripts/db/migrations/audit_003_audit_log_time_range_index.sql");

    @Test
    void migrationMustBeIdempotentAndIndexOnly() throws Exception {
        assertTrue(Files.exists(MIGRATION), "缺少审计时间范围索引迁移: " + MIGRATION.toAbsolutePath());
        String sql = Files.readString(MIGRATION).toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("create index if not exists"), "索引必须使用 IF NOT EXISTS");
        assertTrue(sql.contains("(tenant_id, create_time desc)"), "索引必须覆盖租户与时间范围");
        assertFalse(sql.matches("(?s).*\\bupdate\\b.*"), "迁移不得改写既有数据");
        assertFalse(sql.matches("(?s).*\\bdelete\\s+from\\b.*"), "迁移不得删除既有数据");
    }
}
