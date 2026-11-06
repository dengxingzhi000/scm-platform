-- ======================================================================
-- LMS 运费引擎与对账差异（批次 2）
-- 适用库: db_logistics
-- 执行方式: 幂等，可重复执行；仅新增表/索引，异常中止不删除任何业务数据
-- 前置: 014_db_logistics.sql（tms_waybill / tms_delivery_area）
-- ======================================================================

BEGIN;

-- ----------------------------------------------------------------------
-- 运费模板：按 租户 + 承运商 + 区域（省）配置首重/续重/体积重/区域价
-- region_code = 'DEFAULT' 表示承运商兜底模板
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tms_freight_template (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL,
    template_name        VARCHAR(128),
    carrier_id           UUID NOT NULL,
    region_code          VARCHAR(64) NOT NULL DEFAULT 'DEFAULT',
    region_name          VARCHAR(128),
    first_weight_kg      DECIMAL(10, 3) NOT NULL,
    first_weight_fee     DECIMAL(10, 2) NOT NULL DEFAULT 0,
    additional_unit_kg   DECIMAL(10, 3) NOT NULL DEFAULT 0.5,
    additional_unit_fee  DECIMAL(10, 2) NOT NULL DEFAULT 0,
    volume_divisor       DECIMAL(10, 2) NOT NULL DEFAULT 6000,
    region_surcharge     DECIMAL(10, 2) NOT NULL DEFAULT 0,
    min_charge           DECIMAL(10, 2) NOT NULL DEFAULT 0,
    enabled              BOOLEAN NOT NULL DEFAULT TRUE,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            VARCHAR(64),
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by            VARCHAR(64),
    deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    remark               VARCHAR(512),
    CONSTRAINT chk_freight_first_weight CHECK (first_weight_kg > 0),
    CONSTRAINT chk_freight_additional_unit CHECK (additional_unit_kg > 0),
    CONSTRAINT chk_freight_volume_divisor CHECK (volume_divisor > 0),
    CONSTRAINT chk_freight_fees CHECK (first_weight_fee >= 0 AND additional_unit_fee >= 0
        AND region_surcharge >= 0 AND min_charge >= 0)
);

COMMENT ON TABLE tms_freight_template IS '运费模板：首重续重/体积重/区域价';
COMMENT ON COLUMN tms_freight_template.region_code IS '目的省份编码；DEFAULT 为承运商兜底模板';

-- 同一租户 + 承运商 + 区域仅允许一条未删除模板，避免计费口径歧义
CREATE UNIQUE INDEX IF NOT EXISTS uk_tms_freight_template_scope
    ON tms_freight_template(tenant_id, carrier_id, region_code)
    WHERE deleted = FALSE;

-- ----------------------------------------------------------------------
-- 运费对账：系统应收 vs 承运商账单，差异分类留痕
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tms_freight_reconciliation (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    batch_no          VARCHAR(64) NOT NULL DEFAULT 'DEFAULT',
    waybill_id        UUID NOT NULL,
    waybill_no        VARCHAR(128) NOT NULL,
    carrier_id        UUID,
    region_code       VARCHAR(64),
    actual_weight     DECIMAL(10, 3),
    volume            DECIMAL(10, 2),
    chargeable_weight DECIMAL(10, 3),
    expected_amount   DECIMAL(12, 2) NOT NULL,
    billed_amount     DECIMAL(12, 2) NOT NULL,
    diff_amount       DECIMAL(12, 2) NOT NULL,
    diff_type         VARCHAR(16) NOT NULL,
    status            VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by         VARCHAR(64),
    update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark            VARCHAR(512),
    CONSTRAINT chk_freight_recon_diff_type CHECK (diff_type IN ('MATCH', 'OVERCHARGE', 'UNDERCHARGE')),
    CONSTRAINT chk_freight_recon_status CHECK (status IN ('OPEN', 'CONFIRMED')),
    CONSTRAINT chk_freight_recon_amount CHECK (expected_amount >= 0 AND billed_amount >= 0)
);

COMMENT ON TABLE tms_freight_reconciliation IS '运费对账记录：差异 = 账单金额 - 应收金额，正数为多收';
COMMENT ON COLUMN tms_freight_reconciliation.batch_no IS '对账批次；同批次同运单唯一';

-- 同一批次 + 同一运单只落一条，重复对账由唯一索引兜底（幂等）
CREATE UNIQUE INDEX IF NOT EXISTS uk_tms_freight_recon_batch
    ON tms_freight_reconciliation(tenant_id, waybill_no, batch_no);

CREATE INDEX IF NOT EXISTS idx_tms_freight_recon_diff
    ON tms_freight_reconciliation(tenant_id, diff_type, status);

COMMIT;
