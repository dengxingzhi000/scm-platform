-- ======================================================================
-- 财务批次 5：税率规则表 + 结算单税率列
--
-- 约束：
--   * 幂等：所有 DDL 使用 IF NOT EXISTS，重复执行安全。
--   * 历史不回填：fin_tax_rate 为新表；settlement_order.tax_rate 新增列，
--     历史结算单保持 NULL，表示未采集税率。
--   * 税率以 0..1 小数存储（13% 存 0.1300），与 invoice.tax_rate 口径一致。
--   * 匹配口径见 docs/guides/FINANCE_TAX_RATE_RULES.md：
--     业务日期落在生效区间内的启用规则中，按品类/地区匹配并取
--     effective_from 最新的一条；未命中回退租户默认，再回退配置默认。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 税率规则表
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fin_tax_rate (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    category        VARCHAR(64),
    region          VARCHAR(64),
    rate            DECIMAL(6,4) NOT NULL,
    effective_from  DATE NOT NULL,
    effective_to    DATE,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    remark          TEXT,
    create_time     TIMESTAMP NOT NULL DEFAULT NOW(),
    create_by       UUID,
    update_time     TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by       UUID,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_tax_rate_value CHECK (rate >= 0 AND rate <= 1),
    CONSTRAINT chk_tax_rate_effective_range CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

COMMENT ON TABLE fin_tax_rate IS '税率规则：租户维度，按品类/地区/生效区间匹配';
COMMENT ON COLUMN fin_tax_rate.category IS '品类编码；为空表示不限品类';
COMMENT ON COLUMN fin_tax_rate.region IS '地区编码；为空表示不限地区';
COMMENT ON COLUMN fin_tax_rate.rate IS '税率 0..1，例如 0.1300';
COMMENT ON COLUMN fin_tax_rate.is_default IS '是否为租户默认税率；默认规则不参与品类/地区匹配';

-- 每个租户最多一条启用中的默认规则。
CREATE UNIQUE INDEX IF NOT EXISTS uk_tax_rate_tenant_default
    ON fin_tax_rate(tenant_id) WHERE is_default AND enabled AND NOT deleted;

CREATE INDEX IF NOT EXISTS idx_tax_rate_tenant_lookup
    ON fin_tax_rate(tenant_id, category, region, effective_from DESC) WHERE NOT deleted;

-- ----------------------------------------------------------------------
-- 2. 结算单税率列（历史行保持 NULL，不回填）
-- ----------------------------------------------------------------------
ALTER TABLE settlement_order ADD COLUMN IF NOT EXISTS tax_rate DECIMAL(6,4);
COMMENT ON COLUMN settlement_order.tax_rate IS '适用税率 0..1；NULL 表示历史单据未采集';
