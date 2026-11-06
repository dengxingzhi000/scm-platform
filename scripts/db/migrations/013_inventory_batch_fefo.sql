-- ======================================================================
-- 013 — 批次效期（FEFO）与成本/数量流水批次维度
--
-- 背景
--   FEFO 出库需要按 inv_batch 的 expiry_date 先到期先出，并把批次维度落到
--   inv_cost_log / inv_log。目标库此前从未执行 002_inventory_batch_management.sql，
--   因此本节先补齐 FEFO 所需的最小 inv_batch 结构；002 中的 inv_lot /
--   inv_serial / inv_batch_flow 不在本批范围内。
--
-- 口径
--   - 历史 inv_cost_log / inv_log 行保留 NULL，不回填批次维度。
--   - 可重复执行：CREATE TABLE / INDEX 使用 IF NOT EXISTS，ALTER 使用 ADD COLUMN IF NOT EXISTS。
--   - 不执行任何 DELETE / TRUNCATE / UPDATE 历史数据。
--
-- 执行方式（需用户确认后在 db_inventory 上执行）：
--   psql -d db_inventory -v ON_ERROR_STOP=1 -f 013_inventory_batch_fefo.sql
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. FEFO 最小批次表（结构对齐 002，仅建 FEFO 所需核心列）
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inv_batch (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    batch_code VARCHAR(128) NOT NULL,
    sku_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,

    production_date DATE,
    expiry_date DATE,
    shelf_life_days INT,

    supplier_id UUID,
    supplier_name VARCHAR(256),
    supplier_batch_no VARCHAR(128),

    quality_status SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_batch_quality CHECK (quality_status IN (1, 2, 3, 4)),
    quality_inspector_id UUID,
    quality_inspector_name VARCHAR(128),
    quality_inspected_at TIMESTAMP,
    quality_report JSONB,

    total_quantity INT NOT NULL DEFAULT 0
        CONSTRAINT chk_batch_total CHECK (total_quantity >= 0),
    available_quantity INT NOT NULL DEFAULT 0
        CONSTRAINT chk_batch_available CHECK (available_quantity >= 0),
    locked_quantity INT NOT NULL DEFAULT 0
        CONSTRAINT chk_batch_locked CHECK (locked_quantity >= 0),
    damaged_quantity INT NOT NULL DEFAULT 0
        CONSTRAINT chk_batch_damaged CHECK (damaged_quantity >= 0),

    unit_cost DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_batch_cost CHECK (unit_cost >= 0),

    location_id UUID,
    location_code VARCHAR(64),

    status SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_batch_status CHECK (status IN (0, 1, 2, 3)),

    near_expiry_alert BOOLEAN DEFAULT FALSE,
    expiry_alert_date DATE,

    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT uk_tenant_batch_sku_wh UNIQUE (tenant_id, batch_code, sku_id, warehouse_id),
    CONSTRAINT chk_batch_quantity_balance CHECK (
        total_quantity = available_quantity + locked_quantity + damaged_quantity)
);

CREATE INDEX IF NOT EXISTS idx_batch_tenant
    ON inv_batch(tenant_id, sku_id, warehouse_id) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_batch_code
    ON inv_batch(tenant_id, batch_code);
CREATE INDEX IF NOT EXISTS idx_batch_expiry
    ON inv_batch(expiry_date) WHERE status = 1 AND NOT deleted;
CREATE INDEX IF NOT EXISTS idx_batch_near_expiry
    ON inv_batch(tenant_id) WHERE near_expiry_alert = TRUE AND status = 1;
CREATE INDEX IF NOT EXISTS idx_batch_supplier
    ON inv_batch(tenant_id, supplier_id) WHERE NOT deleted;

COMMENT ON TABLE inv_batch IS '库存批次表（保质期管理）';
COMMENT ON COLUMN inv_batch.quality_status IS '质量状态:1-合格,2-待检,3-不合格,4-隔离';
COMMENT ON COLUMN inv_batch.status IS '批次状态:0-已耗尽,1-正常,2-临期,3-已过期';
COMMENT ON COLUMN inv_batch.near_expiry_alert IS '临期告警（距离过期30天内）';

-- FEFO 排序辅助：同一 SKU/仓库按效期、入库时间（create_time）排序
CREATE INDEX IF NOT EXISTS idx_batch_fefo
    ON inv_batch(tenant_id, sku_id, warehouse_id, expiry_date, create_time)
    WHERE deleted = FALSE AND quality_status = 1 AND available_quantity > 0;

-- ----------------------------------------------------------------------
-- 2. 成本流水批次维度（历史行留空，不回填）
-- ----------------------------------------------------------------------
ALTER TABLE inv_cost_log
    ADD COLUMN IF NOT EXISTS batch_no VARCHAR(128);
ALTER TABLE inv_cost_log
    ADD COLUMN IF NOT EXISTS expiry_date DATE;

COMMENT ON COLUMN inv_cost_log.batch_no IS '批次号；历史行与整批移动为空';
COMMENT ON COLUMN inv_cost_log.expiry_date IS '批次效期；历史行与整批移动为空';

CREATE INDEX IF NOT EXISTS idx_cost_log_batch
    ON inv_cost_log(tenant_id, batch_no) WHERE batch_no IS NOT NULL;

-- ----------------------------------------------------------------------
-- 3. 数量流水批次维度（历史行留空，不回填）
-- ----------------------------------------------------------------------
ALTER TABLE inv_log
    ADD COLUMN IF NOT EXISTS batch_no VARCHAR(128);
ALTER TABLE inv_log
    ADD COLUMN IF NOT EXISTS expiry_date DATE;

COMMENT ON COLUMN inv_log.batch_no IS '批次号；历史行与整批移动为空';
COMMENT ON COLUMN inv_log.expiry_date IS '批次效期；历史行与整批移动为空';

CREATE INDEX IF NOT EXISTS idx_inv_log_batch
    ON inv_log(tenant_id, batch_no) WHERE batch_no IS NOT NULL;
