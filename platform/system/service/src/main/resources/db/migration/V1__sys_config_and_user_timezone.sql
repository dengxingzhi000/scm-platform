-- ============================================================================
-- 平台/租户配置表 + 用户时区列
-- 目标库：db_user（system 主数据源）
-- 幂等：所有对象均使用 IF NOT EXISTS / pg_constraint 判断，bootstrap 与 Flyway 双路径安全
-- ============================================================================

CREATE TABLE IF NOT EXISTS sys_config (
    id           UUID         PRIMARY KEY,
    tenant_id    UUID,
    config_key   VARCHAR(128) NOT NULL,
    config_value TEXT,
    value_type   VARCHAR(16)  NOT NULL DEFAULT 'STRING',
    description  VARCHAR(255),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    UUID,
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    UUID
);

COMMENT ON TABLE sys_config IS '平台级/租户级配置：tenant_id 为空表示平台级默认值，非空表示该租户覆盖值';
COMMENT ON COLUMN sys_config.tenant_id IS 'NULL=平台级；非空=租户覆盖';
COMMENT ON COLUMN sys_config.config_key IS '配置键，同一层级内唯一';
COMMENT ON COLUMN sys_config.value_type IS 'STRING/NUMBER/BOOLEAN/JSON';

-- 平台级（tenant_id IS NULL）按 key 唯一。PostgreSQL 的唯一约束不把多个 NULL 视为相同，
-- 因此必须用部分唯一索引，否则平台级可以写入重复键。
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_config_platform
    ON sys_config (config_key)
    WHERE tenant_id IS NULL AND deleted = FALSE;

-- 租户覆盖按 (tenant_id, config_key) 唯一。
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_config_tenant
    ON sys_config (tenant_id, config_key)
    WHERE tenant_id IS NOT NULL AND deleted = FALSE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_sys_config_value_type'
    ) THEN
        ALTER TABLE sys_config
            ADD CONSTRAINT ck_sys_config_value_type
            CHECK (value_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'JSON'));
    END IF;
END $$;

-- 个人资料自助维护需要的时区列。
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS timezone VARCHAR(64);
COMMENT ON COLUMN sys_user.timezone IS '用户时区（IANA 名称，如 Asia/Shanghai）';
