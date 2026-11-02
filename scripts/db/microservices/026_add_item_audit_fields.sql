-- ============================================================
-- 为入库/出库明细表补充审计字段
-- ============================================================

\c db_warehouse

-- ============================================================
-- wms_inbound_item: 添加 update_time, update_by, create_by
-- ============================================================
ALTER TABLE wms_inbound_item ADD COLUMN IF NOT EXISTS update_time TIMESTAMP;
ALTER TABLE wms_inbound_item ADD COLUMN IF NOT EXISTS update_by VARCHAR(64);
ALTER TABLE wms_inbound_item ADD COLUMN IF NOT EXISTS create_by VARCHAR(64);

COMMENT ON COLUMN wms_inbound_item.update_time IS '更新时间';
COMMENT ON COLUMN wms_inbound_item.update_by IS '更新人';
COMMENT ON COLUMN wms_inbound_item.create_by IS '创建人';

-- ============================================================
-- wms_outbound_item: 添加 update_time, update_by, create_by
-- ============================================================
ALTER TABLE wms_outbound_item ADD COLUMN IF NOT EXISTS update_time TIMESTAMP;
ALTER TABLE wms_outbound_item ADD COLUMN IF NOT EXISTS update_by VARCHAR(64);
ALTER TABLE wms_outbound_item ADD COLUMN IF NOT EXISTS create_by VARCHAR(64);

COMMENT ON COLUMN wms_outbound_item.update_time IS '更新时间';
COMMENT ON COLUMN wms_outbound_item.update_by IS '更新人';
COMMENT ON COLUMN wms_outbound_item.create_by IS '创建人';
