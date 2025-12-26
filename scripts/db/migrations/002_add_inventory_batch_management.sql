-- ======================================================================
-- 库存批次管理扩展脚本
-- 为 db_inventory 添加批次、批号、序列号管理功能
-- 适用场景：食品饮料（保质期）、医药（批号追溯）、电子产品（序列号）
-- ======================================================================

-- 连接到 db_inventory 数据库
-- \c db_inventory

-- ======================================================================
-- 1. 库存批次表 (inv_batch) - 用于保质期管理
-- ======================================================================
CREATE TABLE IF NOT EXISTS inv_batch (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- 批次信息
    batch_code VARCHAR(128) NOT NULL,              -- 批次号
    sku_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,

    -- 生产信息
    production_date DATE,                          -- 生产日期
    expiry_date DATE,                              -- 过期日期
    shelf_life_days INT,                           -- 保质期（天）

    -- 供应商信息
    supplier_id UUID,
    supplier_name VARCHAR(256),
    supplier_batch_no VARCHAR(128),                 -- 供应商批次号

    -- 质量信息
    quality_status SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_batch_quality CHECK (quality_status IN (1, 2, 3, 4)),
    quality_inspector_id UUID,
    quality_inspector_name VARCHAR(128),
    quality_inspected_at TIMESTAMPTZ,
    quality_report JSONB,                          -- 质检报告

    -- 库存数量
    total_quantity INT NOT NULL DEFAULT 0
        CONSTRAINT chk_batch_total CHECK (total_quantity >= 0),
    available_quantity INT NOT NULL DEFAULT 0
        CONSTRAINT chk_batch_available CHECK (available_quantity >= 0),
    locked_quantity INT NOT NULL DEFAULT 0
        CONSTRAINT chk_batch_locked CHECK (locked_quantity >= 0),
    damaged_quantity INT NOT NULL DEFAULT 0
        CONSTRAINT chk_batch_damaged CHECK (damaged_quantity >= 0),

    -- 成本
    unit_cost DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_batch_cost CHECK (unit_cost >= 0),

    -- 库位
    location_id UUID,
    location_code VARCHAR(64),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_batch_status CHECK (status IN (0, 1, 2, 3)),

    -- 告警
    near_expiry_alert BOOLEAN DEFAULT FALSE,       -- 临期告警
    expiry_alert_date DATE,                        -- 告警日期（提前N天）

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT uk_tenant_batch_sku_wh UNIQUE (tenant_id, batch_code, sku_id, warehouse_id),
    CONSTRAINT chk_batch_quantity_balance CHECK (total_quantity = available_quantity + locked_quantity + damaged_quantity)
);

CREATE INDEX idx_batch_tenant ON inv_batch(tenant_id, sku_id, warehouse_id) WHERE NOT deleted;
CREATE INDEX idx_batch_code ON inv_batch(tenant_id, batch_code);
CREATE INDEX idx_batch_expiry ON inv_batch(expiry_date) WHERE status = 1 AND NOT deleted;
CREATE INDEX idx_batch_near_expiry ON inv_batch(tenant_id) WHERE near_expiry_alert = TRUE AND status = 1;
CREATE INDEX idx_batch_supplier ON inv_batch(tenant_id, supplier_id) WHERE NOT deleted;

COMMENT ON TABLE inv_batch IS '库存批次表（保质期管理）';
COMMENT ON COLUMN inv_batch.quality_status IS '质量状态:1-合格,2-待检,3-不合格,4-隔离';
COMMENT ON COLUMN inv_batch.status IS '批次状态:0-已耗尽,1-正常,2-临期,3-已过期';
COMMENT ON COLUMN inv_batch.near_expiry_alert IS '临期告警（距离过期30天内）';

-- ======================================================================
-- 2. 库存批号表 (inv_lot) - 用于批号追溯
-- ======================================================================
CREATE TABLE IF NOT EXISTS inv_lot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- 批号信息
    lot_number VARCHAR(128) NOT NULL,              -- 批号/LOT号
    sku_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    batch_id UUID,                                 -- 关联批次（可选）

    -- 生产追溯信息
    production_line VARCHAR(128),                   -- 生产线
    production_shift VARCHAR(64),                   -- 生产班次
    production_worker VARCHAR(128),                 -- 生产人员
    production_equipment VARCHAR(128),              -- 生产设备
    production_datetime TIMESTAMPTZ,                -- 生产时间

    -- 原料批号（用于向上追溯）
    raw_material_lots JSONB DEFAULT '[]',           -- 原料批号数组 [{skuId, lotNumber, quantity}]

    -- 质量参数
    quality_parameters JSONB DEFAULT '{}',          -- 质量参数 {ph: 7.2, temperature: 25, ...}
    certificate_no VARCHAR(128),                    -- 合格证号

    -- 库存数量
    total_quantity INT NOT NULL DEFAULT 0
        CONSTRAINT chk_lot_total CHECK (total_quantity >= 0),
    available_quantity INT NOT NULL DEFAULT 0
        CONSTRAINT chk_lot_available CHECK (available_quantity >= 0),
    locked_quantity INT NOT NULL DEFAULT 0
        CONSTRAINT chk_lot_locked CHECK (locked_quantity >= 0),

    -- 库位
    location_id UUID,
    location_code VARCHAR(64),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_lot_status CHECK (status IN (0, 1, 2)),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT uk_tenant_lot_number UNIQUE (tenant_id, lot_number, sku_id, warehouse_id)
);

CREATE INDEX idx_lot_tenant ON inv_lot(tenant_id, sku_id, warehouse_id) WHERE NOT deleted;
CREATE INDEX idx_lot_number ON inv_lot(tenant_id, lot_number);
CREATE INDEX idx_lot_batch ON inv_lot(tenant_id, batch_id) WHERE batch_id IS NOT NULL;
CREATE INDEX idx_lot_production_time ON inv_lot(production_datetime DESC);

COMMENT ON TABLE inv_lot IS '库存批号表（批号追溯）';
COMMENT ON COLUMN inv_lot.status IS '批号状态:0-已耗尽,1-正常,2-已召回';
COMMENT ON COLUMN inv_lot.raw_material_lots IS '原料批号（JSONB数组）- 用于向上追溯';

-- ======================================================================
-- 3. 库存序列号表 (inv_serial) - 用于一物一码管理
-- ======================================================================
CREATE TABLE IF NOT EXISTS inv_serial (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- 序列号信息
    serial_number VARCHAR(128) NOT NULL UNIQUE,    -- 序列号/SN/IMEI
    sku_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    batch_id UUID,                                 -- 关联批次（可选）
    lot_number VARCHAR(128),                       -- 关联批号（可选）

    -- 硬件信息（电子产品）
    hardware_info JSONB DEFAULT '{}',              -- {model, version, mac, imei}

    -- 状态
    status SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_serial_status CHECK (status IN (1, 2, 3, 4, 5, 6)),

    -- 库位
    location_id UUID,
    location_code VARCHAR(64),

    -- 销售信息
    sold_to_order_id UUID,
    sold_to_customer_id UUID,
    sold_at TIMESTAMPTZ,

    -- 维修记录
    repair_count INT DEFAULT 0,
    last_repair_at TIMESTAMPTZ,
    warranty_expire_date DATE,

    -- 召回信息
    is_recalled BOOLEAN DEFAULT FALSE,
    recall_reason TEXT,
    recalled_at TIMESTAMPTZ,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX idx_serial_tenant ON inv_serial(tenant_id, sku_id, warehouse_id) WHERE NOT deleted;
CREATE INDEX idx_serial_number ON inv_serial(serial_number);
CREATE INDEX idx_serial_status ON inv_serial(tenant_id, status) WHERE NOT deleted;
CREATE INDEX idx_serial_batch ON inv_serial(tenant_id, batch_id) WHERE batch_id IS NOT NULL;
CREATE INDEX idx_serial_sold_order ON inv_serial(sold_to_order_id) WHERE sold_to_order_id IS NOT NULL;
CREATE INDEX idx_serial_recalled ON inv_serial(tenant_id) WHERE is_recalled = TRUE;

COMMENT ON TABLE inv_serial IS '库存序列号表（一物一码）';
COMMENT ON COLUMN inv_serial.status IS '序列号状态:1-在库,2-已锁定,3-已出库,4-已售出,5-已退货,6-报废';
COMMENT ON COLUMN inv_serial.is_recalled IS '是否召回';

-- ======================================================================
-- 4. 批次流水表 (inv_batch_flow) - 分区表
-- ======================================================================
CREATE TABLE IF NOT EXISTS inv_batch_flow (
    id UUID DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- 批次信息
    batch_id UUID NOT NULL,
    batch_code VARCHAR(128) NOT NULL,
    sku_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,

    -- 操作类型
    operation_type SMALLINT NOT NULL
        CONSTRAINT chk_batch_flow_operation CHECK (operation_type IN (1, 2, 3, 4, 5)),

    -- 数量变化
    quantity INT NOT NULL,
    before_quantity INT NOT NULL
        CONSTRAINT chk_batch_flow_before CHECK (before_quantity >= 0),
    after_quantity INT NOT NULL
        CONSTRAINT chk_batch_flow_after CHECK (after_quantity >= 0),

    -- 引用
    reference_type VARCHAR(64),                    -- INBOUND, OUTBOUND, TRANSFER, ADJUST
    reference_id UUID,
    reference_no VARCHAR(128),

    -- 操作人
    operator_id UUID,
    operator_name VARCHAR(128),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT
) PARTITION BY RANGE (create_time);

-- 创建分区（按月分区）
CREATE TABLE inv_batch_flow_2025_01 PARTITION OF inv_batch_flow
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE inv_batch_flow_2025_02 PARTITION OF inv_batch_flow
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE inv_batch_flow_2025_03 PARTITION OF inv_batch_flow
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE INDEX idx_batch_flow_tenant ON inv_batch_flow(tenant_id, batch_id);
CREATE INDEX idx_batch_flow_sku ON inv_batch_flow(tenant_id, sku_id);
CREATE INDEX idx_batch_flow_time ON inv_batch_flow(create_time DESC);

COMMENT ON TABLE inv_batch_flow IS '批次流水表（分区）';
COMMENT ON COLUMN inv_batch_flow.operation_type IS '操作类型:1-入库,2-出库,3-调整,4-转移,5-报损';

-- ======================================================================
-- 5. 序列号流转记录表 (inv_serial_trace)
-- ======================================================================
CREATE TABLE IF NOT EXISTS inv_serial_trace (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- 序列号
    serial_id UUID NOT NULL,
    serial_number VARCHAR(128) NOT NULL,

    -- 流转信息
    trace_type VARCHAR(64) NOT NULL,               -- INBOUND, OUTBOUND, SOLD, REPAIR, RETURN, RECALL
    from_location VARCHAR(256),
    to_location VARCHAR(256),

    -- 关联单据
    reference_type VARCHAR(64),
    reference_id UUID,
    reference_no VARCHAR(128),

    -- 操作人
    operator_id UUID,
    operator_name VARCHAR(128),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT
);

CREATE INDEX idx_serial_trace_tenant ON inv_serial_trace(tenant_id, serial_id, create_time DESC);
CREATE INDEX idx_serial_trace_number ON inv_serial_trace(serial_number, create_time DESC);
CREATE INDEX idx_serial_trace_type ON inv_serial_trace(trace_type, create_time DESC);

COMMENT ON TABLE inv_serial_trace IS '序列号流转记录表';
COMMENT ON COLUMN inv_serial_trace.trace_type IS '流转类型:INBOUND-入库,OUTBOUND-出库,SOLD-售出,REPAIR-维修,RETURN-退货,RECALL-召回';

-- ======================================================================
-- 视图：临期商品汇总
-- ======================================================================
CREATE OR REPLACE VIEW v_near_expiry_inventory AS
SELECT
    b.tenant_id,
    b.sku_id,
    b.warehouse_id,
    b.batch_code,
    b.expiry_date,
    b.expiry_date - CURRENT_DATE AS days_to_expire,
    b.available_quantity,
    b.unit_cost,
    b.available_quantity * b.unit_cost AS total_value,
    b.supplier_name
FROM inv_batch b
WHERE b.status = 1
  AND b.expiry_date IS NOT NULL
  AND b.expiry_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '30 days'
  AND b.available_quantity > 0
  AND NOT b.deleted
ORDER BY b.expiry_date ASC, b.available_quantity DESC;

COMMENT ON VIEW v_near_expiry_inventory IS '临期商品汇总视图（未来30天过期）';

-- ======================================================================
-- 视图：批次库存汇总（按SKU+仓库）
-- ======================================================================
CREATE OR REPLACE VIEW v_batch_inventory_summary AS
SELECT
    b.tenant_id,
    b.sku_id,
    b.warehouse_id,
    COUNT(b.id) AS batch_count,
    SUM(b.total_quantity) AS total_quantity,
    SUM(b.available_quantity) AS available_quantity,
    SUM(b.locked_quantity) AS locked_quantity,
    SUM(b.damaged_quantity) AS damaged_quantity,
    MIN(b.expiry_date) AS earliest_expiry_date,
    MAX(b.expiry_date) AS latest_expiry_date,
    AVG(b.unit_cost) AS avg_unit_cost
FROM inv_batch b
WHERE b.status = 1
  AND NOT b.deleted
GROUP BY b.tenant_id, b.sku_id, b.warehouse_id;

COMMENT ON VIEW v_batch_inventory_summary IS '批次库存汇总视图（按SKU+仓库）';

-- ======================================================================
-- 函数：自动计算临期告警
-- ======================================================================
CREATE OR REPLACE FUNCTION fn_update_near_expiry_alert()
RETURNS TRIGGER AS $$
DECLARE
    v_alert_days INT := 30;  -- 提前30天告警
BEGIN
    IF NEW.expiry_date IS NOT NULL THEN
        IF NEW.expiry_date - CURRENT_DATE <= v_alert_days THEN
            NEW.near_expiry_alert := TRUE;
            NEW.expiry_alert_date := CURRENT_DATE;
            NEW.status := 2; -- 临期状态
        ELSE
            NEW.near_expiry_alert := FALSE;
            NEW.expiry_alert_date := NULL;
        END IF;

        -- 已过期
        IF NEW.expiry_date < CURRENT_DATE THEN
            NEW.status := 3; -- 已过期
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 触发器：批次插入和更新时自动计算临期告警
CREATE TRIGGER trg_batch_expiry_alert
    BEFORE INSERT OR UPDATE ON inv_batch
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_near_expiry_alert();

COMMENT ON FUNCTION fn_update_near_expiry_alert IS '自动计算批次临期告警';

-- ======================================================================
-- 函数：FEFO（先过期先出）选批
-- ======================================================================
CREATE OR REPLACE FUNCTION fn_pick_batch_fefo(
    p_tenant_id UUID,
    p_sku_id UUID,
    p_warehouse_id UUID,
    p_quantity INT
)
RETURNS TABLE (
    batch_id UUID,
    batch_code VARCHAR,
    expiry_date DATE,
    available_quantity INT,
    pick_quantity INT
) AS $$
DECLARE
    v_remaining INT := p_quantity;
    v_batch RECORD;
BEGIN
    -- 按过期日期升序选择批次
    FOR v_batch IN
        SELECT id, batch_code, expiry_date, available_quantity
        FROM inv_batch
        WHERE tenant_id = p_tenant_id
          AND sku_id = p_sku_id
          AND warehouse_id = p_warehouse_id
          AND status = 1
          AND available_quantity > 0
          AND NOT deleted
        ORDER BY expiry_date ASC NULLS LAST, create_time ASC
    LOOP
        IF v_remaining <= 0 THEN
            EXIT;
        END IF;

        IF v_batch.available_quantity >= v_remaining THEN
            -- 当前批次足够
            batch_id := v_batch.id;
            batch_code := v_batch.batch_code;
            expiry_date := v_batch.expiry_date;
            available_quantity := v_batch.available_quantity;
            pick_quantity := v_remaining;
            RETURN NEXT;
            v_remaining := 0;
        ELSE
            -- 当前批次不足，全部拣选
            batch_id := v_batch.id;
            batch_code := v_batch.batch_code;
            expiry_date := v_batch.expiry_date;
            available_quantity := v_batch.available_quantity;
            pick_quantity := v_batch.available_quantity;
            RETURN NEXT;
            v_remaining := v_remaining - v_batch.available_quantity;
        END IF;
    END LOOP;

    -- 如果库存不足，不会返回错误，由调用方判断
    RETURN;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION fn_pick_batch_fefo IS 'FEFO（先过期先出）批次选择算法';

-- ======================================================================
-- 注意事项：
-- 1. 批次管理适用于有保质期的商品（食品、药品、化妆品等）
-- 2. 批号管理适用于需要质量追溯的商品（医药、食品等）
-- 3. 序列号管理适用于高价值单品（电子产品、奢侈品等）
-- 4. 不是所有SKU都需要批次管理，可通过SKU属性控制
-- 5. FEFO（先过期先出）策略需要在出库时调用 fn_pick_batch_fefo
-- 6. 临期商品需要定时任务扫描并触发告警/促销
-- ======================================================================