-- ======================================================================
-- 物流服务数据库 (db_logistics)
-- 职责：运单管理、物流轨迹、配送商管理、路线规划
-- ======================================================================

-- 创建数据库
-- CREATE DATABASE db_logistics WITH ENCODING = 'UTF8';

-- 启用必要扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
-- CREATE EXTENSION IF NOT EXISTS "postgis";  -- GIS扩展（坐标计算）- 暂时不需要，坐标使用DECIMAL存储

-- ======================================================================
-- 1. 物流商表 (tms_carrier)
-- ======================================================================
CREATE TABLE IF NOT EXISTS tms_carrier (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    carrier_code VARCHAR(64) NOT NULL UNIQUE,
    carrier_name VARCHAR(128) NOT NULL,

    -- 类型
    carrier_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_carrier_type CHECK (carrier_type IN (1, 2, 3, 4)),

    -- 联系方式
    contact_name VARCHAR(128),
    contact_phone VARCHAR(32),
    contact_email VARCHAR(128),
    website VARCHAR(256),

    -- API配置
    api_url VARCHAR(512),
    api_key VARCHAR(256),
    api_secret VARCHAR(256),

    -- 服务范围
    service_area TEXT[],                            -- 服务区域数组
    service_types VARCHAR(64)[],                    -- 服务类型数组

    -- 费率
    base_rate DECIMAL(10, 2)
        CONSTRAINT chk_carrier_rate CHECK (base_rate IS NULL OR base_rate >= 0),

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

CREATE INDEX idx_carrier_code ON tms_carrier(carrier_code) WHERE NOT deleted;
CREATE INDEX idx_carrier_enabled ON tms_carrier(enabled) WHERE NOT deleted;

COMMENT ON TABLE tms_carrier IS '物流商表';
COMMENT ON COLUMN tms_carrier.carrier_type IS '类型:1-快递,2-物流,3-同城配送,4-自营配送';

-- ======================================================================
-- 2. 运单表 (tms_waybill)
-- ======================================================================
CREATE TABLE IF NOT EXISTS tms_waybill (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    waybill_no VARCHAR(128) NOT NULL UNIQUE,        -- 运单号

    -- 物流商
    carrier_id UUID NOT NULL,
    carrier_name VARCHAR(128),

    -- 订单
    order_id UUID NOT NULL,
    order_no VARCHAR(128) NOT NULL,

    -- 发货信息
    sender_name VARCHAR(128),
    sender_phone VARCHAR(32),
    sender_address JSONB,                           -- {province, city, district, detail}

    -- 收货信息
    receiver_name VARCHAR(128) NOT NULL,
    receiver_phone VARCHAR(32) NOT NULL,
    receiver_address JSONB NOT NULL,

    -- 货物信息
    goods_name VARCHAR(256),
    goods_weight DECIMAL(10, 2)
        CONSTRAINT chk_waybill_weight CHECK (goods_weight IS NULL OR goods_weight > 0),
    goods_volume DECIMAL(10, 2)
        CONSTRAINT chk_waybill_volume CHECK (goods_volume IS NULL OR goods_volume > 0),
    goods_value DECIMAL(12, 2)
        CONSTRAINT chk_waybill_value CHECK (goods_value IS NULL OR goods_value >= 0),

    -- 运费
    freight_amount DECIMAL(10, 2) DEFAULT 0
        CONSTRAINT chk_waybill_freight CHECK (freight_amount >= 0),
    insurance_amount DECIMAL(10, 2) DEFAULT 0
        CONSTRAINT chk_waybill_insurance CHECK (insurance_amount >= 0),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_waybill_status CHECK (status IN (0, 1, 2, 3, 4, 5, 6)),

    -- 时间预估
    estimated_delivery TIMESTAMPTZ,
    actual_delivery TIMESTAMPTZ,

    -- 配送员
    courier_id UUID,
    courier_name VARCHAR(128),
    courier_phone VARCHAR(32),

    -- 签收
    sign_type SMALLINT
        CONSTRAINT chk_sign_type CHECK (sign_type IS NULL OR sign_type IN (1, 2, 3)),
    sign_person VARCHAR(128),
    sign_time TIMESTAMPTZ,
    sign_image VARCHAR(512),

    -- 异常
    exception_type VARCHAR(64),
    exception_reason TEXT,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT fk_waybill_carrier FOREIGN KEY (carrier_id) REFERENCES tms_carrier(id)
);

CREATE INDEX idx_waybill_no ON tms_waybill(waybill_no);
CREATE INDEX idx_waybill_order ON tms_waybill(order_id) WHERE NOT deleted;
CREATE INDEX idx_waybill_carrier ON tms_waybill(carrier_id) WHERE NOT deleted;
CREATE INDEX idx_waybill_status ON tms_waybill(status) WHERE NOT deleted;
CREATE INDEX idx_waybill_receiver_phone ON tms_waybill(receiver_phone) WHERE NOT deleted;

COMMENT ON TABLE tms_waybill IS '运单表';
COMMENT ON COLUMN tms_waybill.status IS '状态:0-待揽件,1-已揽件,2-运输中,3-派送中,4-已签收,5-异常,6-退回';
COMMENT ON COLUMN tms_waybill.sign_type IS '签收类型:1-本人签收,2-代签,3-快递柜';

-- ======================================================================
-- 3. 物流轨迹表 (tms_tracking)
-- ======================================================================
CREATE TABLE IF NOT EXISTS tms_tracking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    waybill_id UUID NOT NULL,
    waybill_no VARCHAR(128) NOT NULL,

    -- 轨迹信息
    track_time TIMESTAMPTZ NOT NULL,
    location VARCHAR(256),
    description TEXT NOT NULL,

    -- 位置坐标
    longitude DECIMAL(11, 8),
    latitude DECIMAL(10, 8),

    -- 状态
    track_status VARCHAR(64),

    -- 操作人
    operator VARCHAR(128),

    -- 来源
    source VARCHAR(32) DEFAULT 'API',               -- API, MANUAL

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    extra_data JSONB DEFAULT '{}',

    CONSTRAINT fk_tracking_waybill FOREIGN KEY (waybill_id) REFERENCES tms_waybill(id)
);

CREATE INDEX idx_tracking_waybill ON tms_tracking(waybill_id, track_time DESC);
CREATE INDEX idx_tracking_waybill_no ON tms_tracking(waybill_no);
CREATE INDEX idx_tracking_time ON tms_tracking(track_time DESC);

COMMENT ON TABLE tms_tracking IS '物流轨迹表';
COMMENT ON COLUMN tms_tracking.source IS '来源:API-接口推送,MANUAL-手动录入';

-- ======================================================================
-- 4. 配送路线表 (tms_route)
-- ======================================================================
CREATE TABLE IF NOT EXISTS tms_route (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_no VARCHAR(128) NOT NULL UNIQUE,

    -- 配送日期
    delivery_date DATE NOT NULL,

    -- 配送员
    courier_id UUID,
    courier_name VARCHAR(128),

    -- 车辆
    vehicle_no VARCHAR(64),
    vehicle_type VARCHAR(32),

    -- 路线信息
    start_location VARCHAR(256),
    end_location VARCHAR(256),
    waypoints JSONB,                                -- 途经点数组 [{waybillId, address, longitude, latitude, seq}]

    -- 运单
    waybill_ids UUID[],                             -- 包含的运单ID数组
    waybill_count INT DEFAULT 0
        CONSTRAINT chk_route_count CHECK (waybill_count >= 0),

    -- 距离和时间
    total_distance DECIMAL(10, 2)
        CONSTRAINT chk_route_distance CHECK (total_distance IS NULL OR total_distance >= 0),
    estimated_duration INT,                         -- 预计时长（分钟）
    actual_duration INT,                            -- 实际时长（分钟）

    -- 优化
    optimization_algorithm VARCHAR(32),              -- TSP, GA, ACO
    optimization_score DECIMAL(5, 2),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_route_status CHECK (status IN (0, 1, 2, 3)),

    -- 时间
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    remark TEXT
);

CREATE INDEX idx_route_no ON tms_route(route_no);
CREATE INDEX idx_route_date ON tms_route(delivery_date, status);
CREATE INDEX idx_route_courier ON tms_route(courier_id);
CREATE INDEX idx_route_status ON tms_route(status);

COMMENT ON TABLE tms_route IS '配送路线表';
COMMENT ON COLUMN tms_route.status IS '状态:0-规划中,1-配送中,2-已完成,3-已取消';
COMMENT ON COLUMN tms_route.waypoints IS '途经点（JSONB）- 路径优化后的顺序';
COMMENT ON COLUMN tms_route.optimization_algorithm IS '路径优化算法:TSP-旅行商,GA-遗传,ACO-蚁群';

-- ======================================================================
-- 5. 配送区域表 (tms_delivery_area)
-- ======================================================================
CREATE TABLE IF NOT EXISTS tms_delivery_area (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    area_code VARCHAR(64) NOT NULL UNIQUE,
    area_name VARCHAR(128) NOT NULL,

    -- 覆盖范围
    province VARCHAR(64),
    city VARCHAR(64),
    districts VARCHAR(64)[],                        -- 区县数组

    -- 配送商
    carrier_id UUID,

    -- 时效
    standard_duration INT
        CONSTRAINT chk_area_duration CHECK (standard_duration IS NULL OR standard_duration > 0),
    delivery_type VARCHAR(32),                      -- SAME_DAY, NEXT_DAY, 2_DAY, CUSTOM

    -- 费率
    base_freight DECIMAL(10, 2) DEFAULT 0
        CONSTRAINT chk_area_freight CHECK (base_freight >= 0),
    additional_freight DECIMAL(10, 2) DEFAULT 0
        CONSTRAINT chk_area_additional CHECK (additional_freight >= 0),

    -- 状态
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT fk_area_carrier FOREIGN KEY (carrier_id) REFERENCES tms_carrier(id)
);

CREATE INDEX idx_area_code ON tms_delivery_area(area_code) WHERE NOT deleted;
CREATE INDEX idx_area_carrier ON tms_delivery_area(carrier_id) WHERE NOT deleted;
CREATE INDEX idx_area_enabled ON tms_delivery_area(enabled) WHERE NOT deleted;

COMMENT ON TABLE tms_delivery_area IS '配送区域表';
COMMENT ON COLUMN tms_delivery_area.standard_duration IS '标准配送时长（小时）';

-- ======================================================================
-- 视图：进行中的运单
-- ======================================================================
CREATE OR REPLACE VIEW v_active_waybills AS
SELECT
    w.id,
    w.waybill_no,
    w.order_no,
    w.carrier_name,
    w.receiver_name,
    w.receiver_phone,
    w.status,
    w.estimated_delivery,
    w.create_time,
    (
        SELECT track_time
        FROM tms_tracking t
        WHERE t.waybill_id = w.id
        ORDER BY t.track_time DESC
        LIMIT 1
    ) AS last_track_time
FROM tms_waybill w
WHERE w.status IN (0, 1, 2, 3)  -- 未完成状态
  AND NOT w.deleted
ORDER BY w.create_time DESC;

COMMENT ON VIEW v_active_waybills IS '进行中的运单视图';

-- ======================================================================
-- 注意：
-- 1. 物流轨迹支持API推送和手动录入
-- 2. 路径优化算法在应用层实现（如遗传算法、蚁群算法）
-- 3. PostGIS扩展用于地理坐标计算和距离测算
-- ======================================================================