-- ======================================================================
-- LMS 承运商真实跟踪号（批次 3 补遗）
-- 适用库: db_logistics
-- 执行方式: 幂等，可重复执行；仅新增列/索引
-- 前置: 014_db_logistics.sql（tms_waybill / tms_tracking）
-- ======================================================================

BEGIN;

-- 运单跟踪号：承运商面单/回传的真实跟踪号，与平台运单号可能不同
ALTER TABLE tms_waybill ADD COLUMN IF NOT EXISTS tracking_no VARCHAR(128);
COMMENT ON COLUMN tms_waybill.tracking_no IS '承运商真实跟踪号；首次回传后不覆盖，空值表示承运商尚未回传';

CREATE INDEX IF NOT EXISTS idx_tms_waybill_tracking_no
    ON tms_waybill(tenant_id, tracking_no)
    WHERE tracking_no IS NOT NULL;

-- 轨迹留存回传的跟踪号，便于核查回传来源
ALTER TABLE tms_tracking ADD COLUMN IF NOT EXISTS tracking_no VARCHAR(128);
COMMENT ON COLUMN tms_tracking.tracking_no IS '该条轨迹回传的承运商跟踪号';

COMMIT;
