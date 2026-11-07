-- ============================================================================
-- 用户管理企业级增强 — P0 安全与正确性
-- 目标库：db_user（scm-system 主数据源）
-- 幂等：所有对象均使用 IF NOT EXISTS / IF EXISTS 判断
--
-- 注意：本迁移由 Flyway 在 scm-system 启动时自动执行。执行前必须确认
-- sys_user 中没有重复的 (tenant_id, phone) / (tenant_id, email)，
-- 否则唯一索引创建失败会导致服务启动失败。
-- ============================================================================

-- 1. 乐观锁与账号生命周期字段
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS lock_reason VARCHAR(255);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS status_changed_at TIMESTAMP;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS status_changed_by UUID;

-- 2. 软删除用户名可复用：移除全表唯一约束，保留部分唯一索引
ALTER TABLE sys_user DROP CONSTRAINT IF EXISTS uk_tenant_username;
ALTER TABLE sys_user DROP CONSTRAINT IF EXISTS sys_user_username_key;
DROP INDEX IF EXISTS idx_user_username;
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_username
    ON sys_user (tenant_id, username) WHERE NOT deleted;

-- 3. 租户内手机号/邮箱唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_tenant_phone
    ON sys_user (tenant_id, phone) WHERE NOT deleted AND phone IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_tenant_email
    ON sys_user (tenant_id, email) WHERE NOT deleted AND email IS NOT NULL;

-- 4. 密码历史
CREATE TABLE IF NOT EXISTS sys_user_password_history (
    id            UUID         PRIMARY KEY,
    tenant_id     UUID         NOT NULL,
    user_id       UUID         NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    change_type   VARCHAR(16)  NOT NULL,
    create_by     UUID,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_password_history_user
    ON sys_user_password_history (tenant_id, user_id, create_time DESC);

COMMENT ON TABLE sys_user_password_history IS '用户密码历史，用于禁止复用最近 N 次密码';
COMMENT ON COLUMN sys_user_password_history.change_type IS 'INITIAL / CHANGE / RESET';
