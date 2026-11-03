-- ======================================================================
-- 租户平台服务费账单 (tenant_platform_fee)
-- 由 PlatformFeeCalculationJob 按月生成，保存每个租户的套餐费与资源超量费
-- ======================================================================

CREATE TABLE IF NOT EXISTS tenant_platform_fee (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    billing_period VARCHAR(32) NOT NULL,            -- 2025-01
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    package_id UUID,
    package_name VARCHAR(128),

    base_fee DECIMAL(12, 2) NOT NULL DEFAULT 0
        CONSTRAINT chk_tenant_platform_base_fee CHECK (base_fee >= 0),
    storage_fee DECIMAL(12, 2) NOT NULL DEFAULT 0
        CONSTRAINT chk_tenant_platform_storage_fee CHECK (storage_fee >= 0),
    api_fee DECIMAL(12, 2) NOT NULL DEFAULT 0
        CONSTRAINT chk_tenant_platform_api_fee CHECK (api_fee >= 0),
    total_fee DECIMAL(12, 2) NOT NULL DEFAULT 0
        CONSTRAINT chk_tenant_platform_total_fee CHECK (total_fee >= 0),
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0
        CONSTRAINT chk_tenant_platform_discount CHECK (discount_amount >= 0),
    final_fee DECIMAL(12, 2) NOT NULL DEFAULT 0
        CONSTRAINT chk_tenant_platform_final_fee CHECK (final_fee >= 0),

    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_tenant_platform_status CHECK (status IN (0, 1, 2)),
    paid_at TIMESTAMP,

    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW(),
    remark TEXT,

    CONSTRAINT uk_tenant_platform_fee_period UNIQUE (tenant_id, billing_period)
);

CREATE INDEX idx_tenant_platform_fee_period ON tenant_platform_fee(tenant_id, billing_period);
CREATE INDEX idx_tenant_platform_fee_status ON tenant_platform_fee(tenant_id, status);

COMMENT ON TABLE tenant_platform_fee IS '租户平台服务费账单表';
COMMENT ON COLUMN tenant_platform_fee.status IS '状态:0-待付款,1-已付款,2-已作废';
