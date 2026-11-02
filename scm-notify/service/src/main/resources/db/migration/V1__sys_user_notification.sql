-- ======================================================================
-- 用户站内通知（消息中心）
-- 与 sys_notification_audit（发送审计）分开保存用户可见通知及已读状态。
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_user_notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID,
    title VARCHAR(256) NOT NULL,
    content TEXT,
    type VARCHAR(32) NOT NULL
        CONSTRAINT chk_user_notification_type
        CHECK (type IN ('SYSTEM', 'ORDER', 'INVENTORY', 'APPROVAL', 'SECURITY')),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    link VARCHAR(512),
    read_at TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX IF NOT EXISTS idx_user_notification_tenant
    ON sys_user_notification(tenant_id, is_read, create_time DESC) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_user_notification_user
    ON sys_user_notification(user_id, is_read, create_time DESC) WHERE NOT deleted;

COMMENT ON TABLE sys_user_notification IS '用户站内通知表';
COMMENT ON COLUMN sys_user_notification.type IS '通知类型:SYSTEM,ORDER,INVENTORY,APPROVAL,SECURITY';
COMMENT ON COLUMN sys_user_notification.is_read IS '是否已读';
