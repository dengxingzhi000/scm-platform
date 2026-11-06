-- ======================================================================
-- 012 — 库存移动加权平均成本
--
-- 背景
--   * inv_inventory.average_cost 目前是 DECIMAL(12,2)，只够存金额，不够存
--     单价（移动加权平均要求单价至少 4 位小数）。
--   * 批次 4 的 inv_log 已记录数量前后四元组，但没有单价/金额，无法回答
--     “这次出入库按什么成本计价、结存金额是多少”。
--   * 成本变更必须与库存数量变更同事务，且均价更新不能丢更新。
--
-- 设计
--   1. average_cost 扩到 DECIMAL(18,6)：金额仍按 2 位落库，单价保留 6 位。
--   2. 新增成本流水表 inv_cost_log（普通表，非分区）：
--      - 每次出入库一行，direction + 正数量 + 单价 + 金额；
--      - 记录变更前后均价与结存数量；
--      - 调拨两侧用 counterpart_key 指向对侧流水；
--      - (tenant_id, idempotency_key) 唯一，重复回调/重复 requestId 不重复记账。
--   3. 只加表/改精度，不删列、不重建历史分区，可重复执行。
--
-- 幂等：IF NOT EXISTS / 类型变更可重复执行；无 DELETE/TRUNCATE。
-- 目标库：db_inventory
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 均价精度：DECIMAL(18,6)
-- ----------------------------------------------------------------------
ALTER TABLE inv_inventory
    ALTER COLUMN average_cost TYPE DECIMAL(18, 6);

COMMENT ON COLUMN inv_inventory.average_cost IS
    '移动加权平均单位成本（按 tenant+sku+warehouse 维度），落库精度 6 位小数';

-- ----------------------------------------------------------------------
-- 2. 成本流水表
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inv_cost_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    inventory_id UUID,
    sku_id UUID,
    warehouse_id UUID,
    -- Redis 扣减路径可能只有数字 SKU / 仓库编码
    legacy_sku_id BIGINT,
    warehouse_code VARCHAR(64),

    -- IN=入库（均价按加权公式重算）；OUT=出库（按当时均价，不改均价）
    direction VARCHAR(4) NOT NULL
        CONSTRAINT chk_cost_direction CHECK (direction IN ('IN', 'OUT')),
    quantity INT NOT NULL
        CONSTRAINT chk_cost_quantity CHECK (quantity >= 0),
    unit_price DECIMAL(18, 6) NOT NULL DEFAULT 0
        CONSTRAINT chk_cost_unit_price CHECK (unit_price >= 0),
    amount DECIMAL(18, 2) NOT NULL DEFAULT 0
        CONSTRAINT chk_cost_amount CHECK (amount >= 0),

    before_average_cost DECIMAL(18, 6),
    after_average_cost DECIMAL(18, 6),
    before_balance_qty INT,
    after_balance_qty INT,

    business_type VARCHAR(32),
    business_key VARCHAR(128),
    request_id VARCHAR(128),
    reference_type VARCHAR(32),
    reference_id UUID,
    reference_no VARCHAR(128),
    -- 调拨对侧流水键：TRANSFER_OUT:<单号> / TRANSFER_IN:<单号>
    counterpart_key VARCHAR(128),

    operator_id UUID,
    operator_name VARCHAR(128),
    ledger_source VARCHAR(16) NOT NULL DEFAULT 'DB',
    idempotency_key VARCHAR(256),
    remark VARCHAR(512),
    extra_data JSONB DEFAULT '{}',
    create_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_inv_cost_log_idempotency
    ON inv_cost_log(tenant_id, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_cost_log_tenant_time
    ON inv_cost_log(tenant_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_cost_log_sku_warehouse
    ON inv_cost_log(tenant_id, sku_id, warehouse_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_cost_log_business_key
    ON inv_cost_log(business_key);

COMMENT ON TABLE inv_cost_log IS '库存成本流水：每次出入库的单价/数量/金额与前后均价，与数量变更同事务';
COMMENT ON COLUMN inv_cost_log.direction IS 'IN-入库（重算均价）, OUT-出库（按当时均价，不改均价）';
COMMENT ON COLUMN inv_cost_log.quantity IS '正数量，方向由 direction 表达';
COMMENT ON COLUMN inv_cost_log.unit_price IS '本次计价单价：入库=入库单价；出库=当时移动加权均价';
COMMENT ON COLUMN inv_cost_log.amount IS '本次金额=quantity*unit_price，落库 2 位小数（HALF_UP）';
COMMENT ON COLUMN inv_cost_log.before_average_cost IS '变更前均价';
COMMENT ON COLUMN inv_cost_log.after_average_cost IS '变更后均价';
COMMENT ON COLUMN inv_cost_log.before_balance_qty IS '变更前结存数量（total_stock）';
COMMENT ON COLUMN inv_cost_log.after_balance_qty IS '变更后结存数量（total_stock）';
COMMENT ON COLUMN inv_cost_log.counterpart_key IS '调拨对侧流水键，便于报表按单据串联两侧';
COMMENT ON COLUMN inv_cost_log.ledger_source IS '数量口径来源：DB 或 REDIS（Dubbo 扣减路径）';
COMMENT ON COLUMN inv_cost_log.idempotency_key IS '幂等键：(tenant_id, idempotency_key) 唯一，重复回调不重复记账';
