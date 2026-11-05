-- ======================================================================
-- 财务 P0（批次 1-B）：账期、对账差异、资金流水幂等键、币种
--
-- 约束：
--   * 幂等：所有 DDL 使用 IF NOT EXISTS / 条件判断，重复执行安全。
--   * 遇异常中止：发现历史数据不满足企业级口径（负金额、已付超过应付）时
--     RAISE EXCEPTION，绝不 DELETE/UPDATE 原始数据。
--   * 本脚本只新增结构，不改写既有单据金额。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 0. 历史数据体检：异常即中止（不修改任何数据）
-- ----------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM settlement_order WHERE total_amount < 0) THEN
        RAISE EXCEPTION 'settlement_order 存在负数结算总额，停止迁移，请人工核对';
    END IF;
    IF EXISTS (SELECT 1 FROM settlement_order
               WHERE paid_amount > actual_amount) THEN
        RAISE EXCEPTION 'settlement_order 存在已付金额大于应付金额的记录，停止迁移，请人工核对';
    END IF;
    IF EXISTS (SELECT 1 FROM invoice WHERE amount < 0 OR tax_amount < 0) THEN
        RAISE EXCEPTION 'invoice 存在负数金额，停止迁移，请人工核对';
    END IF;
END $$;

-- ----------------------------------------------------------------------
-- 1. 币种：结算单与发票补 currency（默认 CNY，只允许 ISO-4217 三位字母）
-- ----------------------------------------------------------------------
ALTER TABLE settlement_order ADD COLUMN IF NOT EXISTS currency CHAR(3) NOT NULL DEFAULT 'CNY';
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS currency CHAR(3) NOT NULL DEFAULT 'CNY';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_settlement_currency') THEN
        ALTER TABLE settlement_order
            ADD CONSTRAINT chk_settlement_currency CHECK (currency ~ '^[A-Z]{3}$');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_invoice_currency') THEN
        ALTER TABLE invoice
            ADD CONSTRAINT chk_invoice_currency CHECK (currency ~ '^[A-Z]{3}$');
    END IF;
END $$;

-- ----------------------------------------------------------------------
-- 2. 账期表 fin_accounting_period
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fin_accounting_period (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    period_code CHAR(7) NOT NULL,                  -- YYYY-MM
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_accounting_period_status CHECK (status IN (0, 1)),
    closed_by UUID,
    closed_at TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,
    CONSTRAINT uk_tenant_period_code UNIQUE (tenant_id, period_code),
    CONSTRAINT chk_period_range CHECK (start_date < end_date),
    CONSTRAINT chk_period_code_format CHECK (period_code ~ '^[0-9]{4}-[0-9]{2}$')
);

CREATE INDEX IF NOT EXISTS idx_accounting_period_tenant_status
    ON fin_accounting_period(tenant_id, status) WHERE NOT deleted;

COMMENT ON TABLE fin_accounting_period IS '会计期间（账期）表';
COMMENT ON COLUMN fin_accounting_period.status IS '状态:0-未结账,1-已结账';

-- ----------------------------------------------------------------------
-- 3. 对账差异表 fin_reconciliation_diff（差异处理闭环）
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fin_reconciliation_diff (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    reconciliation_id UUID NOT NULL,
    reconciliation_no VARCHAR(128),
    diff_type VARCHAR(32) NOT NULL,                -- AMOUNT, MISSING_DOC, DUPLICATE, OTHER
    our_amount DECIMAL(15, 2) NOT NULL DEFAULT 0
        CONSTRAINT chk_recon_diff_our CHECK (our_amount >= 0),
    their_amount DECIMAL(15, 2) NOT NULL DEFAULT 0
        CONSTRAINT chk_recon_diff_their CHECK (their_amount >= 0),
    diff_amount DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_recon_diff_amount CHECK (diff_amount >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'CNY'
        CONSTRAINT chk_recon_diff_currency CHECK (currency ~ '^[A-Z]{3}$'),
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_recon_diff_status CHECK (status IN (0, 1, 2)),
    resolution VARCHAR(512),
    resolved_by UUID,
    resolved_at TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX IF NOT EXISTS idx_recon_diff_tenant_reconciliation
    ON fin_reconciliation_diff(tenant_id, reconciliation_id) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_recon_diff_tenant_status
    ON fin_reconciliation_diff(tenant_id, status) WHERE NOT deleted;

COMMENT ON TABLE fin_reconciliation_diff IS '对账差异表（差异逐条处理与留痕）';
COMMENT ON COLUMN fin_reconciliation_diff.status IS '状态:0-待处理,1-已处理,2-已核销豁免';

-- ----------------------------------------------------------------------
-- 4. 资金流水表 fin_fund_flow：租户内 idempotency_key 唯一，防重复入账
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fin_fund_flow (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    flow_no VARCHAR(128) NOT NULL,
    direction VARCHAR(8) NOT NULL
        CONSTRAINT chk_fund_flow_direction CHECK (direction IN ('IN', 'OUT')),
    amount DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_fund_flow_amount CHECK (amount > 0),
    currency CHAR(3) NOT NULL DEFAULT 'CNY'
        CONSTRAINT chk_fund_flow_currency CHECK (currency ~ '^[A-Z]{3}$'),
    settlement_id UUID,
    invoice_id UUID,
    source_type VARCHAR(32) NOT NULL,              -- SETTLEMENT_PAYMENT, INVOICE_PAYMENT, ADJUSTMENT ...
    source_no VARCHAR(128),
    idempotency_key VARCHAR(128) NOT NULL,
    operator_id UUID,
    occur_time TIMESTAMP NOT NULL DEFAULT NOW(),
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    create_by UUID,
    remark TEXT,
    CONSTRAINT uk_tenant_fund_flow_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_fund_flow_tenant_settlement
    ON fin_fund_flow(tenant_id, settlement_id);
CREATE INDEX IF NOT EXISTS idx_fund_flow_tenant_occur
    ON fin_fund_flow(tenant_id, occur_time DESC);

COMMENT ON TABLE fin_fund_flow IS '资金流水表（幂等键唯一，防重复入账）';
COMMENT ON COLUMN fin_fund_flow.idempotency_key IS '租户内唯一的幂等键，重复入账时命中唯一约束';
