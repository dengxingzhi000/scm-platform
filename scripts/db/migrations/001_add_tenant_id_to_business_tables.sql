-- ======================================================================
-- 多租户改造迁移脚本
-- 为所有业务表添加 tenant_id 字段和相关索引
-- 执行顺序：在所有业务数据库初始化后执行
-- ======================================================================

-- ======================================================================
-- 1. 商品服务 (db_product) - 添加 tenant_id
-- ======================================================================

-- 1.1 商品分类表
ALTER TABLE prod_category
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE prod_category
    ADD CONSTRAINT fk_category_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_category_parent;
CREATE INDEX idx_category_parent ON prod_category(tenant_id, parent_id) WHERE NOT deleted;

DROP INDEX IF EXISTS idx_category_code;
CREATE INDEX idx_category_code ON prod_category(tenant_id, category_code) WHERE NOT deleted;

ALTER TABLE prod_category DROP CONSTRAINT IF EXISTS prod_category_category_code_key;
ALTER TABLE prod_category
    ADD CONSTRAINT uk_tenant_category_code UNIQUE (tenant_id, category_code);

-- 1.2 品牌表
ALTER TABLE prod_brand
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE prod_brand
    ADD CONSTRAINT fk_brand_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_brand_code;
CREATE INDEX idx_brand_code ON prod_brand(tenant_id, brand_code) WHERE NOT deleted;

ALTER TABLE prod_brand DROP CONSTRAINT IF EXISTS prod_brand_brand_code_key;
ALTER TABLE prod_brand
    ADD CONSTRAINT uk_tenant_brand_code UNIQUE (tenant_id, brand_code);

-- 1.3 SPU表
ALTER TABLE prod_spu
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE prod_spu
    ADD CONSTRAINT fk_spu_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_spu_code;
CREATE INDEX idx_spu_code ON prod_spu(tenant_id, spu_code) WHERE NOT deleted;

DROP INDEX IF EXISTS idx_spu_category;
CREATE INDEX idx_spu_category ON prod_spu(tenant_id, category_id) WHERE NOT deleted;

DROP INDEX IF EXISTS idx_spu_brand;
CREATE INDEX idx_spu_brand ON prod_spu(tenant_id, brand_id) WHERE NOT deleted;

ALTER TABLE prod_spu DROP CONSTRAINT IF EXISTS prod_spu_spu_code_key;
ALTER TABLE prod_spu
    ADD CONSTRAINT uk_tenant_spu_code UNIQUE (tenant_id, spu_code);

-- 1.4 SKU表
ALTER TABLE prod_sku
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE prod_sku
    ADD CONSTRAINT fk_sku_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_sku_code;
CREATE INDEX idx_sku_code ON prod_sku(tenant_id, sku_code) WHERE NOT deleted;

DROP INDEX IF EXISTS idx_sku_spu;
CREATE INDEX idx_sku_spu ON prod_sku(tenant_id, spu_id) WHERE NOT deleted;

DROP INDEX IF EXISTS idx_sku_barcode;
CREATE INDEX idx_sku_barcode ON prod_sku(tenant_id, barcode) WHERE barcode IS NOT NULL AND NOT deleted;

ALTER TABLE prod_sku DROP CONSTRAINT IF EXISTS prod_sku_sku_code_key;
ALTER TABLE prod_sku
    ADD CONSTRAINT uk_tenant_sku_code UNIQUE (tenant_id, sku_code);

-- 1.5 属性模板表
ALTER TABLE prod_attribute_template
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE prod_attribute_template
    ADD CONSTRAINT fk_attribute_template_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_template_category;
CREATE INDEX idx_template_category ON prod_attribute_template(tenant_id, category_id) WHERE NOT deleted;

-- ======================================================================
-- 2. 库存服务 (db_inventory) - 添加 tenant_id
-- ======================================================================

-- 2.1 库存表
ALTER TABLE inv_inventory
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE inv_inventory
    ADD CONSTRAINT fk_inventory_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_inv_sku;
CREATE INDEX idx_inv_sku ON inv_inventory(tenant_id, sku_id) WHERE NOT deleted;

DROP INDEX IF EXISTS idx_inv_warehouse;
CREATE INDEX idx_inv_warehouse ON inv_inventory(tenant_id, warehouse_id) WHERE NOT deleted;

ALTER TABLE inv_inventory DROP CONSTRAINT IF EXISTS uk_inv_sku_warehouse;
ALTER TABLE inv_inventory
    ADD CONSTRAINT uk_tenant_inv_sku_warehouse UNIQUE (tenant_id, sku_id, warehouse_id);

-- 2.2 库存预占表（分区表）
ALTER TABLE inv_reservation
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

DROP INDEX IF EXISTS idx_reservation_no;
CREATE INDEX idx_reservation_no ON inv_reservation(tenant_id, reservation_no);

DROP INDEX IF EXISTS idx_reservation_order;
CREATE INDEX idx_reservation_order ON inv_reservation(tenant_id, order_id);

DROP INDEX IF EXISTS idx_reservation_sku;
CREATE INDEX idx_reservation_sku ON inv_reservation(tenant_id, sku_id);

-- 2.3 库存日志表（分区表）
ALTER TABLE inv_log
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

DROP INDEX IF EXISTS idx_log_sku;
CREATE INDEX idx_log_sku ON inv_log(tenant_id, sku_id);

DROP INDEX IF EXISTS idx_log_warehouse;
CREATE INDEX idx_log_warehouse ON inv_log(tenant_id, warehouse_id);

-- 2.4 库存快照表
ALTER TABLE inv_snapshot
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE inv_snapshot
    ADD CONSTRAINT fk_snapshot_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_snapshot_sku;
CREATE INDEX idx_snapshot_sku ON inv_snapshot(tenant_id, sku_id, snapshot_date DESC);

DROP INDEX IF EXISTS idx_snapshot_warehouse;
CREATE INDEX idx_snapshot_warehouse ON inv_snapshot(tenant_id, warehouse_id, snapshot_date DESC);

ALTER TABLE inv_snapshot DROP CONSTRAINT IF EXISTS uk_snapshot_date_sku_wh;
ALTER TABLE inv_snapshot
    ADD CONSTRAINT uk_tenant_snapshot_date_sku_wh UNIQUE (tenant_id, snapshot_date, sku_id, warehouse_id);

-- 2.5 库存告警表
ALTER TABLE inv_alert
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE inv_alert
    ADD CONSTRAINT fk_alert_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_alert_sku;
CREATE INDEX idx_alert_sku ON inv_alert(tenant_id, sku_id) WHERE NOT is_resolved;

-- ======================================================================
-- 3. 订单服务 (db_order) - 添加 tenant_id
-- ======================================================================

-- 3.1 订单表（分区表）
ALTER TABLE ord_order
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

DROP INDEX IF EXISTS idx_order_no;
CREATE INDEX idx_order_no ON ord_order(tenant_id, order_no);

DROP INDEX IF EXISTS idx_order_user;
CREATE INDEX idx_order_user ON ord_order(tenant_id, user_id);

DROP INDEX IF EXISTS idx_order_status;
CREATE INDEX idx_order_status ON ord_order(tenant_id, status, create_time DESC);

-- 3.2 订单明细表
ALTER TABLE ord_order_item
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

DROP INDEX IF EXISTS idx_item_order;
CREATE INDEX idx_item_order ON ord_order_item(tenant_id, order_id);

DROP INDEX IF EXISTS idx_item_sku;
CREATE INDEX idx_item_sku ON ord_order_item(tenant_id, sku_id);

-- 3.3 订单状态历史表
ALTER TABLE ord_status_history
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

DROP INDEX IF EXISTS idx_history_order;
CREATE INDEX idx_history_order ON ord_status_history(tenant_id, order_id, transitioned_at DESC);

-- 3.4 支付记录表
ALTER TABLE ord_payment
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

DROP INDEX IF EXISTS idx_payment_no;
CREATE INDEX idx_payment_no ON ord_payment(tenant_id, payment_no);

DROP INDEX IF EXISTS idx_payment_order;
CREATE INDEX idx_payment_order ON ord_payment(tenant_id, order_id);

DROP INDEX IF EXISTS idx_payment_user;
CREATE INDEX idx_payment_user ON ord_payment(tenant_id, user_id);

ALTER TABLE ord_payment DROP CONSTRAINT IF EXISTS ord_payment_payment_no_key;
ALTER TABLE ord_payment
    ADD CONSTRAINT uk_tenant_payment_no UNIQUE (tenant_id, payment_no);

-- 3.5 退款表
ALTER TABLE ord_refund
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

DROP INDEX IF EXISTS idx_refund_no;
CREATE INDEX idx_refund_no ON ord_refund(tenant_id, refund_no);

DROP INDEX IF EXISTS idx_refund_order;
CREATE INDEX idx_refund_order ON ord_refund(tenant_id, order_id);

ALTER TABLE ord_refund DROP CONSTRAINT IF EXISTS ord_refund_refund_no_key;
ALTER TABLE ord_refund
    ADD CONSTRAINT uk_tenant_refund_no UNIQUE (tenant_id, refund_no);

-- ======================================================================
-- 4. 仓储服务 (db_warehouse) - 添加 tenant_id
-- ======================================================================

-- 4.1 仓库表
ALTER TABLE wms_warehouse
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE wms_warehouse
    ADD CONSTRAINT fk_warehouse_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_warehouse_code;
CREATE INDEX idx_warehouse_code ON wms_warehouse(tenant_id, warehouse_code) WHERE NOT deleted;

ALTER TABLE wms_warehouse DROP CONSTRAINT IF EXISTS wms_warehouse_warehouse_code_key;
ALTER TABLE wms_warehouse
    ADD CONSTRAINT uk_tenant_warehouse_code UNIQUE (tenant_id, warehouse_code);

-- 4.2 库位表
ALTER TABLE wms_location
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE wms_location
    ADD CONSTRAINT fk_location_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_location_warehouse;
CREATE INDEX idx_location_warehouse ON wms_location(tenant_id, warehouse_id) WHERE NOT deleted;

ALTER TABLE wms_location DROP CONSTRAINT IF EXISTS uk_warehouse_location;
ALTER TABLE wms_location
    ADD CONSTRAINT uk_tenant_warehouse_location UNIQUE (tenant_id, warehouse_id, location_code);

-- 4.3 入库单表
ALTER TABLE wms_inbound
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE wms_inbound
    ADD CONSTRAINT fk_inbound_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_inbound_no;
CREATE INDEX idx_inbound_no ON wms_inbound(tenant_id, inbound_no);

DROP INDEX IF EXISTS idx_inbound_warehouse;
CREATE INDEX idx_inbound_warehouse ON wms_inbound(tenant_id, warehouse_id) WHERE NOT deleted;

ALTER TABLE wms_inbound DROP CONSTRAINT IF EXISTS wms_inbound_inbound_no_key;
ALTER TABLE wms_inbound
    ADD CONSTRAINT uk_tenant_inbound_no UNIQUE (tenant_id, inbound_no);

-- 4.4 入库单明细表
ALTER TABLE wms_inbound_item
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

DROP INDEX IF EXISTS idx_inbound_item_inbound;
CREATE INDEX idx_inbound_item_inbound ON wms_inbound_item(tenant_id, inbound_id);

-- 4.5 出库单表
ALTER TABLE wms_outbound
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE wms_outbound
    ADD CONSTRAINT fk_outbound_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_outbound_no;
CREATE INDEX idx_outbound_no ON wms_outbound(tenant_id, outbound_no);

DROP INDEX IF EXISTS idx_outbound_warehouse;
CREATE INDEX idx_outbound_warehouse ON wms_outbound(tenant_id, warehouse_id) WHERE NOT deleted;

ALTER TABLE wms_outbound DROP CONSTRAINT IF EXISTS wms_outbound_outbound_no_key;
ALTER TABLE wms_outbound
    ADD CONSTRAINT uk_tenant_outbound_no UNIQUE (tenant_id, outbound_no);

-- 4.6 出库单明细表
ALTER TABLE wms_outbound_item
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

DROP INDEX IF EXISTS idx_outbound_item_outbound;
CREATE INDEX idx_outbound_item_outbound ON wms_outbound_item(tenant_id, outbound_id);

-- 4.7 波次拣货表
ALTER TABLE wms_wave_picking
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE wms_wave_picking
    ADD CONSTRAINT fk_wave_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_wave_no;
CREATE INDEX idx_wave_no ON wms_wave_picking(tenant_id, wave_no);

DROP INDEX IF EXISTS idx_wave_warehouse;
CREATE INDEX idx_wave_warehouse ON wms_wave_picking(tenant_id, warehouse_id);

ALTER TABLE wms_wave_picking DROP CONSTRAINT IF EXISTS wms_wave_picking_wave_no_key;
ALTER TABLE wms_wave_picking
    ADD CONSTRAINT uk_tenant_wave_no UNIQUE (tenant_id, wave_no);

-- ======================================================================
-- 5. 物流服务 (db_logistics) - 添加 tenant_id
-- ======================================================================

-- 5.1 物流商表
ALTER TABLE tms_carrier
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE tms_carrier
    ADD CONSTRAINT fk_carrier_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_carrier_code;
CREATE INDEX idx_carrier_code ON tms_carrier(tenant_id, carrier_code) WHERE NOT deleted;

ALTER TABLE tms_carrier DROP CONSTRAINT IF EXISTS tms_carrier_carrier_code_key;
ALTER TABLE tms_carrier
    ADD CONSTRAINT uk_tenant_carrier_code UNIQUE (tenant_id, carrier_code);

-- 5.2 运单表
ALTER TABLE tms_waybill
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE tms_waybill
    ADD CONSTRAINT fk_waybill_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_waybill_no;
CREATE INDEX idx_waybill_no ON tms_waybill(tenant_id, waybill_no);

DROP INDEX IF EXISTS idx_waybill_order;
CREATE INDEX idx_waybill_order ON tms_waybill(tenant_id, order_id) WHERE NOT deleted;

ALTER TABLE tms_waybill DROP CONSTRAINT IF EXISTS tms_waybill_waybill_no_key;
ALTER TABLE tms_waybill
    ADD CONSTRAINT uk_tenant_waybill_no UNIQUE (tenant_id, waybill_no);

-- 5.3 物流轨迹表
ALTER TABLE tms_tracking
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

DROP INDEX IF EXISTS idx_tracking_waybill;
CREATE INDEX idx_tracking_waybill ON tms_tracking(tenant_id, waybill_id, track_time DESC);

-- 5.4 配送路线表
ALTER TABLE tms_route
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE tms_route
    ADD CONSTRAINT fk_route_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_route_no;
CREATE INDEX idx_route_no ON tms_route(tenant_id, route_no);

ALTER TABLE tms_route DROP CONSTRAINT IF EXISTS tms_route_route_no_key;
ALTER TABLE tms_route
    ADD CONSTRAINT uk_tenant_route_no UNIQUE (tenant_id, route_no);

-- 5.5 配送区域表
ALTER TABLE tms_delivery_area
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE tms_delivery_area
    ADD CONSTRAINT fk_delivery_area_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_area_code;
CREATE INDEX idx_area_code ON tms_delivery_area(tenant_id, area_code) WHERE NOT deleted;

ALTER TABLE tms_delivery_area DROP CONSTRAINT IF EXISTS tms_delivery_area_area_code_key;
ALTER TABLE tms_delivery_area
    ADD CONSTRAINT uk_tenant_area_code UNIQUE (tenant_id, area_code);

-- ======================================================================
-- 6. 供应商服务 (db_supplier) - 添加 tenant_id
-- ======================================================================

-- 6.1 供应商表
ALTER TABLE sup_supplier
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE sup_supplier
    ADD CONSTRAINT fk_supplier_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_supplier_code;
CREATE INDEX idx_supplier_code ON sup_supplier(tenant_id, supplier_code) WHERE NOT deleted;

ALTER TABLE sup_supplier DROP CONSTRAINT IF EXISTS sup_supplier_supplier_code_key;
ALTER TABLE sup_supplier
    ADD CONSTRAINT uk_tenant_supplier_code UNIQUE (tenant_id, supplier_code);

-- 6.2 采购单表（分区表）
ALTER TABLE sup_purchase_order
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

DROP INDEX IF EXISTS idx_purchase_no;
CREATE INDEX idx_purchase_no ON sup_purchase_order(tenant_id, purchase_no);

DROP INDEX IF EXISTS idx_purchase_supplier;
CREATE INDEX idx_purchase_supplier ON sup_purchase_order(tenant_id, supplier_id) WHERE NOT deleted;

-- 6.3 采购单明细表
ALTER TABLE sup_purchase_order_item
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

DROP INDEX IF EXISTS idx_purchase_item_purchase;
CREATE INDEX idx_purchase_item_purchase ON sup_purchase_order_item(tenant_id, purchase_id);

-- 6.4 供应商评价表
ALTER TABLE sup_supplier_evaluation
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE sup_supplier_evaluation
    ADD CONSTRAINT fk_evaluation_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_evaluation_supplier;
CREATE INDEX idx_evaluation_supplier ON sup_supplier_evaluation(tenant_id, supplier_id);

ALTER TABLE sup_supplier_evaluation DROP CONSTRAINT IF EXISTS uk_supplier_period;
ALTER TABLE sup_supplier_evaluation
    ADD CONSTRAINT uk_tenant_supplier_period UNIQUE (tenant_id, supplier_id, evaluation_period);

-- 6.5 对账单表
ALTER TABLE sup_settlement
    ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL;

ALTER TABLE sup_settlement
    ADD CONSTRAINT fk_settlement_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

DROP INDEX IF EXISTS idx_settlement_no;
CREATE INDEX idx_settlement_no ON sup_settlement(tenant_id, settlement_no);

DROP INDEX IF EXISTS idx_settlement_supplier;
CREATE INDEX idx_settlement_supplier ON sup_settlement(tenant_id, supplier_id) WHERE NOT deleted;

ALTER TABLE sup_settlement DROP CONSTRAINT IF EXISTS sup_settlement_settlement_no_key;
ALTER TABLE sup_settlement
    ADD CONSTRAINT uk_tenant_settlement_no UNIQUE (tenant_id, settlement_no);

-- ======================================================================
-- 注意事项：
-- 1. 执行此脚本前，确保已创建 db_tenant 数据库并初始化 tenant 表
-- 2. 由于添加了 NOT NULL 约束，执行前需要：
--    方案A：先添加为 NULL，插入默认租户数据，再改为 NOT NULL
--    方案B：如果是新系统，直接执行即可
-- 3. 所有查询必须添加 tenant_id 过滤条件，建议在应用层使用拦截器自动注入
-- 4. 分区表的tenant_id需要在创建新分区时包含
-- 5. 考虑使用 PostgreSQL Row Level Security (RLS) 增强数据隔离
-- ======================================================================