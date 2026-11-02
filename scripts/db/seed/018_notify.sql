-- ======================================================================
-- 本地开发种子数据 018 — 站内通知（消息中心）
-- 目标库：db_notify
-- ======================================================================
BEGIN;

-- 旧库可能还没有 Flyway V1 建的表，这里再兜底一次。
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
    business_key VARCHAR(128),
    read_at TIMESTAMPTZ,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX IF NOT EXISTS idx_user_notification_tenant
    ON sys_user_notification(tenant_id, is_read, create_time DESC) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_user_notification_user
    ON sys_user_notification(user_id, is_read, create_time DESC) WHERE NOT deleted;
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_notification_business
    ON sys_user_notification(tenant_id, user_id, business_key)
    WHERE business_key IS NOT NULL AND NOT deleted;

DELETE FROM sys_user_notification WHERE id IN (
    '019a0aee-0000-7000-8000-000000000c01',
    '019a0aee-0000-7000-8000-000000000c02',
    '019a0aee-0000-7000-8000-000000000c03',
    '019a0aee-0000-7000-8000-000000000c04',
    '019a0aee-0000-7000-8000-000000000c05',
    '019a0aee-0000-7000-8000-000000000c06',
    '019a0aee-0000-7000-8000-000000000c07',
    '019a0aee-0000-7000-8000-000000000c08',
    '019a0aee-0000-7000-8000-000000000d01',
    '019a0aee-0000-7000-8000-000000000d02',
    '019a0aee-0000-7000-8000-000000000d03',
    '019a0aee-0000-7000-8000-000000000d04'
);

INSERT INTO sys_user_notification (
    id, tenant_id, user_id, title, content, type, is_read, link, read_at,
    create_time, update_time, deleted, remark
) VALUES
    ('019a0aee-0000-7000-8000-000000000c01', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000101', '订单已发货', '订单 ORD-2026-1003 已发货，运单号 WB-2026-1003', 'ORDER', FALSE, '/order', NULL, NOW() - INTERVAL '2 hours', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000c02', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000101', '库存预警', 'AirPods Pro 2 可用库存仅剩 3 件，低于安全库存 20', 'INVENTORY', FALSE, '/inventory/alerts', NULL, NOW() - INTERVAL '3 hours', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000c03', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000101', '系统维护通知', '系统将于今晚 22:00-23:00 进行维护升级，请提前保存工作', 'SYSTEM', TRUE, NULL, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000c04', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000102', '采购单审批通过', '采购单 PO-2026-1003 已通过审批，等待供应商发货', 'APPROVAL', FALSE, '/purchase', NULL, NOW() - INTERVAL '1 day', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000c05', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000102', '登录异常', '您的账号在新设备登录，如非本人操作请及时修改密码', 'SECURITY', FALSE, NULL, NULL, NOW() - INTERVAL '2 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000c06', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000103', '退款申请待审核', '订单 ORD-2026-1013 发起退款申请，金额 ¥6,499', 'APPROVAL', FALSE, '/order/refund', NULL, NOW() - INTERVAL '2 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000c07', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000103', '库存同步完成', '主仓库库存数据已同步至缓存，共更新 8 个 SKU', 'SYSTEM', TRUE, NULL, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000c08', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000104', '支付成功', '订单 ORD-2026-1020 支付成功，实付 ¥1,899', 'ORDER', TRUE, '/order', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000d01', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000001', '库存缺货提醒', '华为 Mate 60 Pro 256G 可用库存为 0，请及时补货', 'INVENTORY', FALSE, '/inventory/alerts', NULL, NOW() - INTERVAL '40 minutes', NOW(), FALSE, '管理员演示数据'),
    ('019a0aee-0000-7000-8000-000000000d02', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000001', '订单待发货', '订单 ORD-2026-1023 已支付，等待仓库发货', 'ORDER', FALSE, '/order', NULL, NOW() - INTERVAL '2 hours', NOW(), FALSE, '管理员演示数据'),
    ('019a0aee-0000-7000-8000-000000000d03', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000001', '退款申请待审核', '订单 ORD-2026-1029 发起退款申请，金额 ¥9,999', 'APPROVAL', FALSE, '/order/refund', NULL, NOW() - INTERVAL '5 hours', NOW(), FALSE, '管理员演示数据'),
    ('019a0aee-0000-7000-8000-000000000d04', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000001', '系统维护通知', '系统将于今晚 22:00-23:00 进行维护升级', 'SYSTEM', TRUE, NULL, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW(), FALSE, '管理员演示数据');

COMMIT;
