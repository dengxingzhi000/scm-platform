-- ============================================================================
-- 用户管理企业级增强 — 二次验证恢复码
-- 目标库：db_user
-- 幂等：IF NOT EXISTS
-- ============================================================================

CREATE TABLE IF NOT EXISTS sys_user_recovery_code (
    id          UUID        PRIMARY KEY,
    tenant_id   UUID        NOT NULL,
    user_id     UUID        NOT NULL,
    code_hash   CHAR(64)    NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT FALSE,
    used_at     TIMESTAMP,
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_recovery_code
    ON sys_user_recovery_code (tenant_id, user_id, code_hash) WHERE NOT used;

CREATE INDEX IF NOT EXISTS idx_user_recovery_code_user
    ON sys_user_recovery_code (tenant_id, user_id, used);

COMMENT ON TABLE sys_user_recovery_code IS 'TOTP 二次验证一次性恢复码（SHA-256 哈希存储）';
