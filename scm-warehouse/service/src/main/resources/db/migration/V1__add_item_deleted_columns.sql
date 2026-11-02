-- ======================================================================
-- 入库/出库明细实体声明了逻辑删除字段 deleted，但建表脚本漏了这两列。
-- 查询明细（wms-inbound-item/list、wms-outbound-item/list）会拼上
-- deleted = false，缺失列时直接报 SQL 错误。这里补齐，幂等。
-- ======================================================================
ALTER TABLE wms_inbound_item ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE wms_outbound_item ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_inbound_item_deleted ON wms_inbound_item(deleted) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_outbound_item_deleted ON wms_outbound_item(deleted) WHERE NOT deleted;
