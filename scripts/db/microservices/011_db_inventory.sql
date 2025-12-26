-- ======================================================================
-- 库存服务数据库 (db_inventory)
-- 职责：库存管理、预占、释放、库存日志、库存快照
-- ======================================================================

-- 创建数据库
-- CREATE DATABASE db_inventory WITH ENCODING = 'UTF8';

-- 启用必要扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ======================================================================
-- 1. 库存表 (inv_inventory)
-- ======================================================================
CREATE TABLE IF NOT EXISTS inv_inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id UUID NOT NULL,                          -- 关联 db_product.prod_sku.id
    warehouse_id UUID NOT NULL,                    -- 关联 db_warehouse.wms_warehouse.id

    -- 库存数量
    total_stock INT NOT NULL DEFAULT 0
        CONSTRAINT chk_inv_total_stock CHECK (total_stock >= 0),
    available_stock INT NOT NULL DEFAULT 0
        CONSTRAINT chk_inv_available_stock CHECK (available_stock >= 0),
    locked_stock INT NOT NULL DEFAULT 0
        CONSTRAINT chk_inv_locked_stock CHECK (locked_stock >= 0),
    damaged_stock INT NOT NULL DEFAULT 0
        CONSTRAINT chk_inv_damaged_stock CHECK (damaged_stock >= 0),

    -- 安全库存
    safety_stock INT DEFAULT 0
        CONSTRAINT chk_inv_safety_stock CHECK (safety_stock >= 0),
    max_stock INT
        CONSTRAINT chk_inv_max_stock CHECK (max_stock IS NULL OR max_stock > 0),

    -- 库位
    location_code VARCHAR(64),

    -- 成本
    average_cost DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_inv_average_cost CHECK (average_cost >= 0),

    -- 乐观锁
    version INT NOT NULL DEFAULT 0,

    -- 最近操作时间
    last_inbound_at TIMESTAMPTZ,
    last_outbound_at TIMESTAMPTZ,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT uk_inv_sku_warehouse UNIQUE (sku_id, warehouse_id),
    CONSTRAINT chk_inv_stock_balance CHECK (total_stock = available_stock + locked_stock + damaged_stock)
);

CREATE INDEX idx_inv_sku ON inv_inventory(sku_id) WHERE NOT deleted;
CREATE INDEX idx_inv_warehouse ON inv_inventory(warehouse_id) WHERE NOT deleted;
CREATE INDEX idx_inv_available ON inv_inventory(available_stock) WHERE available_stock > 0 AND NOT deleted;
CREATE INDEX idx_inv_low_stock ON inv_inventory(sku_id) WHERE available_stock <= safety_stock AND NOT deleted;

COMMENT ON TABLE inv_inventory IS '库存表';
COMMENT ON COLUMN inv_inventory.available_stock IS '可用库存 = 总库存 - 锁定库存 - 损坏库存';
COMMENT ON COLUMN inv_inventory.locked_stock IS '锁定库存（预占）';
COMMENT ON COLUMN inv_inventory.safety_stock IS '安全库存（低于此值告警）';
COMMENT ON COLUMN inv_inventory.version IS '乐观锁版本号';

-- ======================================================================
-- 2. 库存预占表 (inv_reservation) - 分区表
-- ======================================================================
CREATE TABLE IF NOT EXISTS inv_reservation (
    id UUID DEFAULT gen_random_uuid(),
    reservation_no VARCHAR(128) NOT NULL,   -- 预占单号

    -- SKU和仓库
    sku_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,

    -- 订单信息
    order_id UUID NOT NULL,
    order_no VARCHAR(128) NOT NULL,
    user_id UUID,

    -- 数量
    quantity INT NOT NULL
        CONSTRAINT chk_reservation_quantity CHECK (quantity > 0),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_reservation_status CHECK (status IN (0, 1, 2, 3)),

    -- 时间
    expire_at TIMESTAMPTZ NOT NULL,
    reserved_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMPTZ,
    released_at TIMESTAMPTZ,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT,

    -- UNIQUE constraint must include partition key for partitioned tables
    CONSTRAINT uk_reservation_no_reserved_at UNIQUE (reservation_no, reserved_at)
) PARTITION BY RANGE (reserved_at);

-- 创建分区（按月分区）
CREATE TABLE inv_reservation_2025_01 PARTITION OF inv_reservation
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE inv_reservation_2025_02 PARTITION OF inv_reservation
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE inv_reservation_2025_03 PARTITION OF inv_reservation
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE INDEX idx_reservation_no ON inv_reservation(reservation_no);
CREATE INDEX idx_reservation_order ON inv_reservation(order_id);
CREATE INDEX idx_reservation_sku ON inv_reservation(sku_id);
CREATE INDEX idx_reservation_status ON inv_reservation(status);
CREATE INDEX idx_reservation_expire ON inv_reservation(expire_at) WHERE status = 0;

COMMENT ON TABLE inv_reservation IS '库存预占表（分区）';
COMMENT ON COLUMN inv_reservation.status IS '状态:0-已预占,1-已确认,2-已释放,3-已过期';
COMMENT ON COLUMN inv_reservation.expire_at IS '过期时间（15分钟后自动释放）';

-- ======================================================================
-- 3. 库存日志表 (inv_log) - 分区表
-- ======================================================================
CREATE TABLE IF NOT EXISTS inv_log (
    id UUID DEFAULT gen_random_uuid(),

    -- SKU和仓库
    sku_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,

    -- 操作类型
    operation_type SMALLINT NOT NULL
        CONSTRAINT chk_log_operation CHECK (operation_type IN (1, 2, 3, 4, 5, 6, 7)),

    -- 数量变化
    quantity INT NOT NULL,
    before_stock INT NOT NULL
        CONSTRAINT chk_log_before_stock CHECK (before_stock >= 0),
    after_stock INT NOT NULL
        CONSTRAINT chk_log_after_stock CHECK (after_stock >= 0),

    -- 引用
    reference_type VARCHAR(64),                    -- ORDER, PURCHASE, RETURN, TRANSFER, ADJUST
    reference_id UUID,
    reference_no VARCHAR(128),

    -- 操作人
    operator_id UUID,
    operator_name VARCHAR(128),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT,
    extra_data JSONB DEFAULT '{}'
) PARTITION BY RANGE (create_time);

-- 创建分区（按月分区）
CREATE TABLE inv_log_2025_01 PARTITION OF inv_log
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE inv_log_2025_02 PARTITION OF inv_log
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE inv_log_2025_03 PARTITION OF inv_log
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE INDEX idx_log_sku ON inv_log(sku_id);
CREATE INDEX idx_log_warehouse ON inv_log(warehouse_id);
CREATE INDEX idx_log_operation ON inv_log(operation_type);
CREATE INDEX idx_log_reference ON inv_log(reference_type, reference_id);
CREATE INDEX idx_log_time ON inv_log(create_time DESC);

COMMENT ON TABLE inv_log IS '库存变动日志表（分区）';
COMMENT ON COLUMN inv_log.operation_type IS '操作类型:1-入库,2-出库,3-预占,4-释放,5-确认,6-调整,7-调拨';

-- ======================================================================
-- 4. 库存快照表 (inv_snapshot)
-- ======================================================================
CREATE TABLE IF NOT EXISTS inv_snapshot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_date DATE NOT NULL,
    sku_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,

    -- 快照时的库存
    total_stock INT NOT NULL
        CONSTRAINT chk_snapshot_total CHECK (total_stock >= 0),
    available_stock INT NOT NULL
        CONSTRAINT chk_snapshot_available CHECK (available_stock >= 0),
    locked_stock INT NOT NULL
        CONSTRAINT chk_snapshot_locked CHECK (locked_stock >= 0),
    damaged_stock INT NOT NULL
        CONSTRAINT chk_snapshot_damaged CHECK (damaged_stock >= 0),

    -- 成本
    average_cost DECIMAL(12, 2),
    total_value DECIMAL(15, 2),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_snapshot_date_sku_wh UNIQUE (snapshot_date, sku_id, warehouse_id)
);

CREATE INDEX idx_snapshot_date ON inv_snapshot(snapshot_date DESC);
CREATE INDEX idx_snapshot_sku ON inv_snapshot(sku_id, snapshot_date DESC);
CREATE INDEX idx_snapshot_warehouse ON inv_snapshot(warehouse_id, snapshot_date DESC);

COMMENT ON TABLE inv_snapshot IS '库存快照表（每日快照）';

-- ======================================================================
-- 5. 库存告警表 (inv_alert)
-- ======================================================================
CREATE TABLE IF NOT EXISTS inv_alert (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- SKU和仓库
    sku_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,

    -- 告警类型
    alert_type SMALLINT NOT NULL
        CONSTRAINT chk_alert_type CHECK (alert_type IN (1, 2, 3, 4)),

    -- 告警详情
    current_stock INT,
    safety_stock INT,
    threshold_value INT,

    -- 状态
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMPTZ,
    resolved_by UUID,

    -- 通知
    notified BOOLEAN NOT NULL DEFAULT FALSE,
    notified_at TIMESTAMPTZ,
    notify_to_user_ids UUID[],

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT
);

CREATE INDEX idx_alert_sku ON inv_alert(sku_id) WHERE NOT is_resolved;
CREATE INDEX idx_alert_type ON inv_alert(alert_type) WHERE NOT is_resolved;
CREATE INDEX idx_alert_unresolved ON inv_alert(is_resolved, create_time);

COMMENT ON TABLE inv_alert IS '库存告警表';
COMMENT ON COLUMN inv_alert.alert_type IS '告警类型:1-库存不足,2-库存为0,3-即将过期,4-损坏';

-- ======================================================================
-- 函数：原子扣减库存（使用乐观锁）
-- ======================================================================
CREATE OR REPLACE FUNCTION fn_deduct_stock(
    p_sku_id UUID,
    p_warehouse_id UUID,
    p_quantity INT,
    p_order_id UUID,
    p_order_no VARCHAR,
    p_operator_id UUID
)
RETURNS JSONB AS $$
DECLARE
    v_current_version INT;
    v_available_stock INT;
    v_result JSONB;
BEGIN
    -- 锁定行并获取当前版本和库存
    SELECT version, available_stock
    INTO v_current_version, v_available_stock
    FROM inv_inventory
    WHERE sku_id = p_sku_id AND warehouse_id = p_warehouse_id AND NOT deleted
    FOR UPDATE;

    -- 检查库存是否存在
    IF NOT FOUND THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'INVENTORY_NOT_FOUND',
            'message', '库存记录不存在'
        );
    END IF;

    -- 检查库存是否充足
    IF v_available_stock < p_quantity THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'INSUFFICIENT_STOCK',
            'message', '库存不足',
            'available', v_available_stock,
            'requested', p_quantity
        );
    END IF;

    -- 扣减库存（可用库存转为锁定库存）
    UPDATE inv_inventory
    SET
        available_stock = available_stock - p_quantity,
        locked_stock = locked_stock + p_quantity,
        version = version + 1,
        last_outbound_at = NOW(),
        update_time = NOW()
    WHERE sku_id = p_sku_id
      AND warehouse_id = p_warehouse_id
      AND version = v_current_version
      AND NOT deleted;

    -- 记录日志
    INSERT INTO inv_log (
        sku_id, warehouse_id, operation_type, quantity,
        before_stock, after_stock,
        reference_type, reference_id, reference_no,
        operator_id
    ) VALUES (
        p_sku_id, p_warehouse_id, 3, p_quantity,
        v_available_stock, v_available_stock - p_quantity,
        'ORDER', p_order_id, p_order_no,
        p_operator_id
    );

    RETURN jsonb_build_object(
        'success', true,
        'before_stock', v_available_stock,
        'after_stock', v_available_stock - p_quantity,
        'deducted', p_quantity
    );
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION fn_deduct_stock IS '原子扣减库存（带乐观锁）';

-- ======================================================================
-- 函数：释放预占库存
-- ======================================================================
CREATE OR REPLACE FUNCTION fn_release_reservation(
    p_reservation_id UUID,
    p_operator_id UUID
)
RETURNS JSONB AS $$
DECLARE
    v_reservation RECORD;
BEGIN
    -- 获取预占记录
    SELECT * INTO v_reservation
    FROM inv_reservation
    WHERE id = p_reservation_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'RESERVATION_NOT_FOUND');
    END IF;

    IF v_reservation.status != 0 THEN
        RETURN jsonb_build_object('success', false, 'error', 'INVALID_STATUS');
    END IF;

    -- 释放库存（锁定库存转回可用库存）
    UPDATE inv_inventory
    SET
        available_stock = available_stock + v_reservation.quantity,
        locked_stock = locked_stock - v_reservation.quantity,
        update_time = NOW()
    WHERE sku_id = v_reservation.sku_id
      AND warehouse_id = v_reservation.warehouse_id
      AND NOT deleted;

    -- 更新预占状态
    UPDATE inv_reservation
    SET status = 2, released_at = NOW()
    WHERE id = p_reservation_id;

    -- 记录日志
    INSERT INTO inv_log (
        sku_id, warehouse_id, operation_type, quantity,
        before_stock, after_stock,
        reference_type, reference_id, reference_no,
        operator_id, remark
    )
    SELECT
        v_reservation.sku_id,
        v_reservation.warehouse_id,
        4,
        v_reservation.quantity,
        i.available_stock - v_reservation.quantity,
        i.available_stock,
        'ORDER',
        v_reservation.order_id,
        v_reservation.order_no,
        p_operator_id,
        '释放预占: ' || v_reservation.reservation_no
    FROM inv_inventory i
    WHERE i.sku_id = v_reservation.sku_id
      AND i.warehouse_id = v_reservation.warehouse_id
      AND NOT i.deleted;

    RETURN jsonb_build_object('success', true, 'released_quantity', v_reservation.quantity);
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION fn_release_reservation IS '释放预占库存';

-- ======================================================================
-- 视图：库存摘要（含库存状态）
-- ======================================================================
CREATE OR REPLACE VIEW v_inventory_summary AS
SELECT
    i.id,
    i.sku_id,
    i.warehouse_id,
    i.total_stock,
    i.available_stock,
    i.locked_stock,
    i.damaged_stock,
    i.safety_stock,
    CASE
        WHEN i.available_stock = 0 THEN 'OUT_OF_STOCK'
        WHEN i.available_stock <= i.safety_stock THEN 'LOW_STOCK'
        ELSE 'NORMAL'
    END AS stock_status,
    i.update_time
FROM inv_inventory i
WHERE NOT i.deleted;

COMMENT ON VIEW v_inventory_summary IS '库存摘要视图（含库存状态）';

-- ======================================================================
-- 注意：
-- 1. update_time 由 MyBatis-Plus 自动填充
-- 2. 库存操作建议使用Redis Lua脚本处理高并发场景
-- 3. 数据库函数作为兜底方案，保证数据一致性
-- ======================================================================