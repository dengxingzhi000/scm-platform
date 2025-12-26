-- ======================================================================
-- 仓储服务数据库 (db_warehouse)
-- 职责：仓库管理、入库、出库、库位、波次拣货
-- ======================================================================

-- 创建数据库
-- CREATE DATABASE db_warehouse WITH ENCODING = 'UTF8';

-- 启用必要扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ======================================================================
-- 1. 仓库表 (wms_warehouse)
-- ======================================================================
CREATE TABLE IF NOT EXISTS wms_warehouse (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_code VARCHAR(64) NOT NULL UNIQUE,
    warehouse_name VARCHAR(128) NOT NULL,
    warehouse_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_warehouse_type CHECK (warehouse_type IN (1, 2, 3, 4)),

    -- 地址
    province VARCHAR(64),
    city VARCHAR(64),
    district VARCHAR(64),
    address TEXT,
    longitude DECIMAL(11, 8),
    latitude DECIMAL(10, 8),

    -- 联系
    manager_id UUID,
    manager_name VARCHAR(128),
    phone VARCHAR(32),
    email VARCHAR(128),

    -- 容量
    total_capacity INT
        CONSTRAINT chk_warehouse_capacity CHECK (total_capacity IS NULL OR total_capacity > 0),
    used_capacity INT DEFAULT 0
        CONSTRAINT chk_warehouse_used CHECK (used_capacity >= 0),

    -- 状态
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT DEFAULT 0,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX idx_warehouse_code ON wms_warehouse(warehouse_code) WHERE NOT deleted;
CREATE INDEX idx_warehouse_enabled ON wms_warehouse(enabled) WHERE NOT deleted;

COMMENT ON TABLE wms_warehouse IS '仓库表';
COMMENT ON COLUMN wms_warehouse.warehouse_type IS '仓库类型:1-中心仓,2-区域仓,3-前置仓,4-虚拟仓';

-- ======================================================================
-- 2. 库位表 (wms_location)
-- ======================================================================
CREATE TABLE IF NOT EXISTS wms_location (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL,
    location_code VARCHAR(64) NOT NULL,            -- 库位编码 如: A01-01-01

    -- 层级
    zone VARCHAR(32),                               -- 区域：A, B, C
    shelf VARCHAR(32),                              -- 货架：01, 02
    layer VARCHAR(32),                              -- 层：01, 02, 03
    position VARCHAR(32),                           -- 位：01, 02

    -- 类型
    location_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_location_type CHECK (location_type IN (1, 2, 3, 4)),

    -- 容量
    max_capacity INT
        CONSTRAINT chk_location_capacity CHECK (max_capacity IS NULL OR max_capacity > 0),
    current_capacity INT DEFAULT 0
        CONSTRAINT chk_location_current CHECK (current_capacity >= 0),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_location_status CHECK (status IN (0, 1, 2)),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT fk_location_warehouse FOREIGN KEY (warehouse_id) REFERENCES wms_warehouse(id),
    CONSTRAINT uk_warehouse_location UNIQUE (warehouse_id, location_code)
);

CREATE INDEX idx_location_warehouse ON wms_location(warehouse_id) WHERE NOT deleted;
CREATE INDEX idx_location_code ON wms_location(location_code);
CREATE INDEX idx_location_status ON wms_location(status) WHERE NOT deleted;

COMMENT ON TABLE wms_location IS '库位表';
COMMENT ON COLUMN wms_location.location_type IS '库位类型:1-普通,2-冷藏,3-冷冻,4-危险品';
COMMENT ON COLUMN wms_location.status IS '状态:0-锁定,1-可用,2-维护中';

-- ======================================================================
-- 3. 入库单表 (wms_inbound)
-- ======================================================================
CREATE TABLE IF NOT EXISTS wms_inbound (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inbound_no VARCHAR(128) NOT NULL UNIQUE,

    -- 仓库
    warehouse_id UUID NOT NULL,

    -- 类型和来源
    inbound_type SMALLINT NOT NULL
        CONSTRAINT chk_inbound_type CHECK (inbound_type IN (1, 2, 3, 4)),
    source_type VARCHAR(32),                        -- PURCHASE, RETURN, TRANSFER
    source_id UUID,
    source_no VARCHAR(128),

    -- 供应商
    supplier_id UUID,

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_inbound_status CHECK (status IN (0, 1, 2, 3, 4)),

    -- 数量
    total_quantity INT DEFAULT 0
        CONSTRAINT chk_inbound_quantity CHECK (total_quantity >= 0),
    received_quantity INT DEFAULT 0
        CONSTRAINT chk_inbound_received CHECK (received_quantity >= 0),

    -- 时间
    expected_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    -- 操作人
    operator_id UUID,
    operator_name VARCHAR(128),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT fk_inbound_warehouse FOREIGN KEY (warehouse_id) REFERENCES wms_warehouse(id)
);

CREATE INDEX idx_inbound_no ON wms_inbound(inbound_no);
CREATE INDEX idx_inbound_warehouse ON wms_inbound(warehouse_id) WHERE NOT deleted;
CREATE INDEX idx_inbound_status ON wms_inbound(status) WHERE NOT deleted;
CREATE INDEX idx_inbound_source ON wms_inbound(source_type, source_id);

COMMENT ON TABLE wms_inbound IS '入库单表';
COMMENT ON COLUMN wms_inbound.inbound_type IS '入库类型:1-采购入库,2-退货入库,3-调拨入库,4-其他入库';
COMMENT ON COLUMN wms_inbound.status IS '状态:0-待入库,1-入库中,2-部分入库,3-已完成,4-已取消';

-- ======================================================================
-- 4. 入库单明细表 (wms_inbound_item)
-- ======================================================================
CREATE TABLE IF NOT EXISTS wms_inbound_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inbound_id UUID NOT NULL,
    inbound_no VARCHAR(128) NOT NULL,

    -- SKU
    sku_id UUID NOT NULL,
    sku_code VARCHAR(128),
    sku_name VARCHAR(256),

    -- 库位
    location_id UUID,
    location_code VARCHAR(64),

    -- 数量
    plan_quantity INT NOT NULL
        CONSTRAINT chk_inbound_item_plan CHECK (plan_quantity > 0),
    actual_quantity INT DEFAULT 0
        CONSTRAINT chk_inbound_item_actual CHECK (actual_quantity >= 0),

    -- 质量
    quality_status SMALLINT DEFAULT 1
        CONSTRAINT chk_quality_status CHECK (quality_status IN (1, 2, 3)),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT,

    CONSTRAINT fk_inbound_item FOREIGN KEY (inbound_id) REFERENCES wms_inbound(id)
);

CREATE INDEX idx_inbound_item_inbound ON wms_inbound_item(inbound_id);
CREATE INDEX idx_inbound_item_sku ON wms_inbound_item(sku_id);

COMMENT ON TABLE wms_inbound_item IS '入库单明细表';
COMMENT ON COLUMN wms_inbound_item.quality_status IS '质量状态:1-合格,2-待检,3-不合格';

-- ======================================================================
-- 5. 出库单表 (wms_outbound)
-- ======================================================================
CREATE TABLE IF NOT EXISTS wms_outbound (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outbound_no VARCHAR(128) NOT NULL UNIQUE,

    -- 仓库
    warehouse_id UUID NOT NULL,

    -- 类型和来源
    outbound_type SMALLINT NOT NULL
        CONSTRAINT chk_outbound_type CHECK (outbound_type IN (1, 2, 3, 4)),
    source_type VARCHAR(32),                        -- ORDER, TRANSFER, SCRAP
    source_id UUID,
    source_no VARCHAR(128),

    -- 优先级
    priority SMALLINT DEFAULT 1
        CONSTRAINT chk_outbound_priority CHECK (priority IN (1, 2, 3)),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_outbound_status CHECK (status IN (0, 1, 2, 3, 4)),

    -- 数量
    total_quantity INT DEFAULT 0
        CONSTRAINT chk_outbound_quantity CHECK (total_quantity >= 0),
    picked_quantity INT DEFAULT 0
        CONSTRAINT chk_outbound_picked CHECK (picked_quantity >= 0),

    -- 拣货路径优化
    picking_path JSONB,                             -- [{locationCode, skuId, quantity, distance}]
    total_distance INT,                             -- 总距离（米）

    -- 时间
    expected_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    -- 操作人
    picker_id UUID,
    picker_name VARCHAR(128),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT fk_outbound_warehouse FOREIGN KEY (warehouse_id) REFERENCES wms_warehouse(id)
);

CREATE INDEX idx_outbound_no ON wms_outbound(outbound_no);
CREATE INDEX idx_outbound_warehouse ON wms_outbound(warehouse_id) WHERE NOT deleted;
CREATE INDEX idx_outbound_status ON wms_outbound(status) WHERE NOT deleted;
CREATE INDEX idx_outbound_priority ON wms_outbound(priority, create_time);
CREATE INDEX idx_outbound_source ON wms_outbound(source_type, source_id);

COMMENT ON TABLE wms_outbound IS '出库单表';
COMMENT ON COLUMN wms_outbound.outbound_type IS '出库类型:1-销售出库,2-调拨出库,3-报损出库,4-其他出库';
COMMENT ON COLUMN wms_outbound.priority IS '优先级:1-普通,2-紧急,3-特急';
COMMENT ON COLUMN wms_outbound.status IS '状态:0-待拣货,1-拣货中,2-已拣货,3-已出库,4-已取消';
COMMENT ON COLUMN wms_outbound.picking_path IS '拣货路径优化（JSONB）';

-- ======================================================================
-- 6. 出库单明细表 (wms_outbound_item)
-- ======================================================================
CREATE TABLE IF NOT EXISTS wms_outbound_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outbound_id UUID NOT NULL,
    outbound_no VARCHAR(128) NOT NULL,

    -- SKU
    sku_id UUID NOT NULL,
    sku_code VARCHAR(128),
    sku_name VARCHAR(256),

    -- 库位
    location_id UUID,
    location_code VARCHAR(64),

    -- 数量
    plan_quantity INT NOT NULL
        CONSTRAINT chk_outbound_item_plan CHECK (plan_quantity > 0),
    actual_quantity INT DEFAULT 0
        CONSTRAINT chk_outbound_item_actual CHECK (actual_quantity >= 0),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT,

    CONSTRAINT fk_outbound_item FOREIGN KEY (outbound_id) REFERENCES wms_outbound(id)
);

CREATE INDEX idx_outbound_item_outbound ON wms_outbound_item(outbound_id);
CREATE INDEX idx_outbound_item_sku ON wms_outbound_item(sku_id);

COMMENT ON TABLE wms_outbound_item IS '出库单明细表';

-- ======================================================================
-- 7. 波次拣货表 (wms_wave_picking)
-- ======================================================================
CREATE TABLE IF NOT EXISTS wms_wave_picking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wave_no VARCHAR(128) NOT NULL UNIQUE,

    -- 仓库
    warehouse_id UUID NOT NULL,

    -- 出库单
    outbound_ids UUID[],                            -- 合并拣货的出库单ID数组
    order_count INT DEFAULT 0
        CONSTRAINT chk_wave_order_count CHECK (order_count >= 0),

    -- 拣货信息
    total_items INT DEFAULT 0
        CONSTRAINT chk_wave_items CHECK (total_items >= 0),
    picking_path JSONB,                             -- 优化后的拣货路径
    total_distance INT,                             -- 总距离（米）
    optimization_rate DECIMAL(5, 2),                 -- 优化率（%）

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_wave_status CHECK (status IN (0, 1, 2, 3)),

    -- 拣货人
    picker_id UUID,
    picker_name VARCHAR(128),

    -- 时间
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    remark TEXT,

    CONSTRAINT fk_wave_warehouse FOREIGN KEY (warehouse_id) REFERENCES wms_warehouse(id)
);

CREATE INDEX idx_wave_no ON wms_wave_picking(wave_no);
CREATE INDEX idx_wave_warehouse ON wms_wave_picking(warehouse_id);
CREATE INDEX idx_wave_status ON wms_wave_picking(status);

COMMENT ON TABLE wms_wave_picking IS '波次拣货表';
COMMENT ON COLUMN wms_wave_picking.status IS '状态:0-待拣货,1-拣货中,2-已完成,3-已取消';
COMMENT ON COLUMN wms_wave_picking.optimization_rate IS '路径优化率';

-- ======================================================================
-- 注意：
-- 1. update_time 由 MyBatis-Plus 自动填充
-- 2. 拣货路径优化算法在应用层实现（如TSP问题）
-- 3. 支持RFID/条码扫描入库出库
-- ======================================================================