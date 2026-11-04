-- ======================================================================
-- LMS 物流轨迹回调幂等（批次 1）
-- 适用库: db_logistics
-- 执行方式: 幂等，可重复执行；异常中止不删除任何业务数据
-- 前置: 014_db_logistics.sql 已创建 tms_tracking；028 若尚未执行，本脚本会补齐 tenant_id 列
-- ======================================================================

BEGIN;

-- 幂等字段
ALTER TABLE tms_tracking ADD COLUMN IF NOT EXISTS event_id VARCHAR(128);
ALTER TABLE tms_tracking ADD COLUMN IF NOT EXISTS payload_hash VARCHAR(64);

-- 028 尚未执行时补齐租户列（不创建租户索引，避免与 028 重复）
ALTER TABLE tms_tracking ADD COLUMN IF NOT EXISTS tenant_id UUID;

COMMENT ON COLUMN tms_tracking.event_id IS '承运商回调事件 ID，同租户内唯一，用于幂等';
COMMENT ON COLUMN tms_tracking.payload_hash IS '轨迹内容 SHA-256 指纹，未传事件 ID 时作为幂等键';

-- 同一租户下同一事件只允许落库一次；并发重复回调由该唯一索引兜底
CREATE UNIQUE INDEX IF NOT EXISTS uk_tms_tracking_event
    ON tms_tracking(tenant_id, event_id)
    WHERE event_id IS NOT NULL;

COMMIT;
