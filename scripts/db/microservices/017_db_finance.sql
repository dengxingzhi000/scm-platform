-- ======================================================================
-- 财务服务数据库 (db_finance)
-- 职责：运费管理、结算对账、发票管理、付款记录、平台服务费
-- ======================================================================

-- 创建数据库
-- CREATE DATABASE db_finance WITH ENCODING = 'UTF8';

-- 启用必要扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ======================================================================
-- 1. 运费规则表 (freight_rule)
-- ======================================================================
CREATE TABLE IF NOT EXISTS freight_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,

    -- 适用范围
    carrier_id UUID,                                -- 物流商
    carrier_name VARCHAR(128),
    region_type VARCHAR(32),                        -- PROVINCE, CITY, DISTRICT
    regions TEXT[],                                 -- 适用地区数组

    -- 计费类型
    billing_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_freight_billing_type CHECK (billing_type IN (1, 2, 3, 4)),

    -- 首重/首件
    first_weight DECIMAL(10, 2) DEFAULT 1
        CONSTRAINT chk_freight_first_weight CHECK (first_weight > 0),
    first_price DECIMAL(10, 2) DEFAULT 0
        CONSTRAINT chk_freight_first_price CHECK (first_price >= 0),

    -- 续重/续件
    additional_weight DECIMAL(10, 2) DEFAULT 1
        CONSTRAINT chk_freight_additional_weight CHECK (additional_weight > 0),
    additional_price DECIMAL(10, 2) DEFAULT 0
        CONSTRAINT chk_freight_additional_price CHECK (additional_price >= 0),

    -- 按金额免运费
    free_threshold DECIMAL(12, 2)
        CONSTRAINT chk_freight_threshold CHECK (free_threshold IS NULL OR free_threshold >= 0),

    -- 固定运费
    fixed_freight DECIMAL(10, 2)
        CONSTRAINT chk_freight_fixed CHECK (fixed_freight IS NULL OR fixed_freight >= 0),

    -- 附加费用
    remote_area_fee DECIMAL(10, 2) DEFAULT 0
        CONSTRAINT chk_freight_remote CHECK (remote_area_fee >= 0),
    handling_fee DECIMAL(10, 2) DEFAULT 0
        CONSTRAINT chk_freight_handling CHECK (handling_fee >= 0),

    -- 优先级
    priority INT DEFAULT 0,

    -- 生效时间
    effective_date DATE NOT NULL,
    expiry_date DATE,

    -- 状态
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT uk_tenant_freight_rule_code UNIQUE (tenant_id, rule_code)
);

CREATE INDEX idx_freight_rule_tenant ON freight_rule(tenant_id, enabled) WHERE NOT deleted;
CREATE INDEX idx_freight_rule_carrier ON freight_rule(tenant_id, carrier_id) WHERE NOT deleted;
CREATE INDEX idx_freight_rule_effective ON freight_rule(effective_date, expiry_date);

COMMENT ON TABLE freight_rule IS '运费规则表';
COMMENT ON COLUMN freight_rule.billing_type IS '计费类型:1-按重量,2-按件数,3-按体积,4-固定运费';
COMMENT ON COLUMN freight_rule.free_threshold IS '满额免运费阈值';

-- ======================================================================
-- 2. 运费计算记录表 (freight_calc_record) - 分区表
-- ======================================================================
CREATE TABLE IF NOT EXISTS freight_calc_record (
    id UUID DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- 订单信息
    order_id UUID NOT NULL,
    order_no VARCHAR(128) NOT NULL,

    -- 规则
    rule_id UUID,
    rule_code VARCHAR(64),

    -- 计算参数
    total_weight DECIMAL(10, 2),
    total_volume DECIMAL(10, 2),
    total_quantity INT,
    order_amount DECIMAL(12, 2),

    -- 收货地址
    receiver_province VARCHAR(64),
    receiver_city VARCHAR(64),
    receiver_district VARCHAR(64),

    -- 计算结果
    base_freight DECIMAL(10, 2) DEFAULT 0
        CONSTRAINT chk_calc_base_freight CHECK (base_freight >= 0),
    remote_area_fee DECIMAL(10, 2) DEFAULT 0
        CONSTRAINT chk_calc_remote_fee CHECK (remote_area_fee >= 0),
    handling_fee DECIMAL(10, 2) DEFAULT 0
        CONSTRAINT chk_calc_handling_fee CHECK (handling_fee >= 0),
    discount_amount DECIMAL(10, 2) DEFAULT 0
        CONSTRAINT chk_calc_discount CHECK (discount_amount >= 0),
    final_freight DECIMAL(10, 2) NOT NULL
        CONSTRAINT chk_calc_final_freight CHECK (final_freight >= 0),

    -- 是否免运费
    is_free BOOLEAN DEFAULT FALSE,
    free_reason VARCHAR(256),

    -- 计算详情
    calc_detail JSONB,                              -- 计算过程详情

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (create_time);

-- 创建分区（按月分区）
CREATE TABLE freight_calc_record_2025_01 PARTITION OF freight_calc_record
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE freight_calc_record_2025_02 PARTITION OF freight_calc_record
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE freight_calc_record_2025_03 PARTITION OF freight_calc_record
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE INDEX idx_freight_calc_tenant ON freight_calc_record(tenant_id, order_id);
CREATE INDEX idx_freight_calc_time ON freight_calc_record(create_time DESC);

COMMENT ON TABLE freight_calc_record IS '运费计算记录表（分区）';

-- ======================================================================
-- 3. 结算单表 (settlement_order)
-- ======================================================================
CREATE TABLE IF NOT EXISTS settlement_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    settlement_no VARCHAR(128) NOT NULL,

    -- 结算类型
    settlement_type SMALLINT NOT NULL
        CONSTRAINT chk_settlement_type CHECK (settlement_type IN (1, 2, 3)),

    -- 结算对象
    partner_type VARCHAR(32) NOT NULL,              -- SUPPLIER, CUSTOMER, CARRIER
    partner_id UUID NOT NULL,
    partner_name VARCHAR(256),

    -- 结算周期
    settlement_period VARCHAR(32) NOT NULL,         -- 2025-01, 2025Q1
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    -- 金额
    total_amount DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_settlement_total CHECK (total_amount >= 0),
    discount_amount DECIMAL(15, 2) DEFAULT 0
        CONSTRAINT chk_settlement_discount CHECK (discount_amount >= 0),
    adjustment_amount DECIMAL(15, 2) DEFAULT 0,     -- 可正可负
    actual_amount DECIMAL(15, 2) NOT NULL,

    -- 已付款金额
    paid_amount DECIMAL(15, 2) DEFAULT 0
        CONSTRAINT chk_settlement_paid CHECK (paid_amount >= 0),
    unpaid_amount DECIMAL(15, 2) DEFAULT 0
        CONSTRAINT chk_settlement_unpaid CHECK (unpaid_amount >= 0),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_settlement_status CHECK (status IN (0, 1, 2, 3, 4)),

    -- 审批
    approver_id UUID,
    approver_name VARCHAR(128),
    approved_at TIMESTAMPTZ,

    -- 附件
    attachments JSONB DEFAULT '[]',                 -- 附件URL数组

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT uk_tenant_settlement_no UNIQUE (tenant_id, settlement_no)
);

CREATE INDEX idx_settlement_tenant ON settlement_order(tenant_id, settlement_type) WHERE NOT deleted;
CREATE INDEX idx_settlement_partner ON settlement_order(tenant_id, partner_type, partner_id) WHERE NOT deleted;
CREATE INDEX idx_settlement_period ON settlement_order(tenant_id, settlement_period);
CREATE INDEX idx_settlement_status ON settlement_order(tenant_id, status) WHERE NOT deleted;

COMMENT ON TABLE settlement_order IS '结算单表';
COMMENT ON COLUMN settlement_order.settlement_type IS '结算类型:1-采购结算,2-销售结算,3-物流结算';
COMMENT ON COLUMN settlement_order.partner_type IS '结算对象:SUPPLIER-供应商,CUSTOMER-客户,CARRIER-物流商';
COMMENT ON COLUMN settlement_order.status IS '状态:0-待确认,1-已确认,2-待付款,3-部分付款,4-已付款';

-- ======================================================================
-- 4. 结算明细表 (settlement_item)
-- ======================================================================
CREATE TABLE IF NOT EXISTS settlement_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    settlement_id UUID NOT NULL,
    settlement_no VARCHAR(128) NOT NULL,

    -- 单据信息
    document_type VARCHAR(32) NOT NULL,             -- ORDER, PURCHASE, WAYBILL
    document_id UUID NOT NULL,
    document_no VARCHAR(128) NOT NULL,
    document_date DATE,

    -- 金额
    amount DECIMAL(15, 2) NOT NULL,
    discount DECIMAL(15, 2) DEFAULT 0
        CONSTRAINT chk_settlement_item_discount CHECK (discount >= 0),
    actual_amount DECIMAL(15, 2) NOT NULL,

    -- 状态
    is_settled BOOLEAN DEFAULT FALSE,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT,

    CONSTRAINT fk_settlement_item FOREIGN KEY (settlement_id) REFERENCES settlement_order(id)
);

CREATE INDEX idx_settlement_item_settlement ON settlement_item(tenant_id, settlement_id);
CREATE INDEX idx_settlement_item_document ON settlement_item(tenant_id, document_type, document_id);

COMMENT ON TABLE settlement_item IS '结算明细表';
COMMENT ON COLUMN settlement_item.document_type IS '单据类型:ORDER-订单,PURCHASE-采购单,WAYBILL-运单';

-- ======================================================================
-- 5. 发票表 (invoice)
-- ======================================================================
CREATE TABLE IF NOT EXISTS invoice (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    invoice_no VARCHAR(128) NOT NULL,

    -- 发票类型
    invoice_type SMALLINT NOT NULL
        CONSTRAINT chk_invoice_type CHECK (invoice_type IN (1, 2, 3)),
    invoice_kind SMALLINT NOT NULL
        CONSTRAINT chk_invoice_kind CHECK (invoice_kind IN (1, 2)),

    -- 开票对象
    party_type VARCHAR(32) NOT NULL,                -- CUSTOMER, SUPPLIER
    party_id UUID NOT NULL,
    party_name VARCHAR(256) NOT NULL,
    party_tax_no VARCHAR(64) NOT NULL,

    -- 发票信息
    invoice_code VARCHAR(64),                       -- 发票代码
    invoice_number VARCHAR(64),                     -- 发票号码
    invoice_date DATE,

    -- 金额
    amount DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_invoice_amount CHECK (amount > 0),
    tax_amount DECIMAL(15, 2) DEFAULT 0
        CONSTRAINT chk_invoice_tax CHECK (tax_amount >= 0),
    total_amount DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_invoice_total CHECK (total_amount >= 0),
    tax_rate DECIMAL(5, 4) DEFAULT 0
        CONSTRAINT chk_invoice_tax_rate CHECK (tax_rate >= 0 AND tax_rate <= 1),

    -- 关联单据
    related_orders UUID[],                          -- 关联订单ID数组
    settlement_id UUID,                             -- 关联结算单

    -- 开票信息
    issuer_name VARCHAR(128),
    issue_date DATE,

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_invoice_status CHECK (status IN (0, 1, 2, 3, 4)),

    -- 附件
    invoice_file_url VARCHAR(512),                  -- 发票文件
    invoice_pdf_url VARCHAR(512),                   -- PDF版本

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT uk_tenant_invoice_no UNIQUE (tenant_id, invoice_no)
);

CREATE INDEX idx_invoice_tenant ON invoice(tenant_id, invoice_type) WHERE NOT deleted;
CREATE INDEX idx_invoice_party ON invoice(tenant_id, party_type, party_id) WHERE NOT deleted;
CREATE INDEX idx_invoice_date ON invoice(invoice_date DESC);
CREATE INDEX idx_invoice_status ON invoice(tenant_id, status) WHERE NOT deleted;

COMMENT ON TABLE invoice IS '发票表';
COMMENT ON COLUMN invoice.invoice_type IS '发票类型:1-增值税普通发票,2-增值税专用发票,3-电子发票';
COMMENT ON COLUMN invoice.invoice_kind IS '发票方向:1-销项发票,2-进项发票';
COMMENT ON COLUMN invoice.status IS '状态:0-草稿,1-已开具,2-已邮寄,3-已作废,4-已红冲';

-- ======================================================================
-- 6. 付款记录表 (payment_record) - 分区表
-- ======================================================================
CREATE TABLE IF NOT EXISTS payment_record (
    id UUID DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    payment_no VARCHAR(128) NOT NULL,

    -- 付款类型
    payment_type SMALLINT NOT NULL
        CONSTRAINT chk_payment_record_type CHECK (payment_type IN (1, 2, 3)),
    payment_direction SMALLINT NOT NULL
        CONSTRAINT chk_payment_direction CHECK (payment_direction IN (1, 2)),

    -- 付款对象
    party_type VARCHAR(32) NOT NULL,
    party_id UUID NOT NULL,
    party_name VARCHAR(256),

    -- 关联单据
    settlement_id UUID,
    settlement_no VARCHAR(128),
    invoice_id UUID,

    -- 金额
    payment_amount DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_payment_record_amount CHECK (payment_amount > 0),

    -- 付款方式
    payment_method SMALLINT NOT NULL
        CONSTRAINT chk_payment_record_method CHECK (payment_method IN (1, 2, 3, 4, 5)),
    payment_channel VARCHAR(64),

    -- 银行信息
    payer_bank VARCHAR(128),
    payer_account VARCHAR(64),
    payee_bank VARCHAR(128),
    payee_account VARCHAR(64),

    -- 第三方信息
    third_party_no VARCHAR(256),
    third_party_response JSONB,

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_payment_record_status CHECK (status IN (0, 1, 2, 3)),

    -- 时间
    payment_date DATE NOT NULL,
    completed_at TIMESTAMPTZ,

    -- 审批
    approver_id UUID,
    approver_name VARCHAR(128),
    approved_at TIMESTAMPTZ,

    -- 凭证
    voucher_url VARCHAR(512),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    remark TEXT
) PARTITION BY RANGE (create_time);

-- 创建分区（按月分区）
CREATE TABLE payment_record_2025_01 PARTITION OF payment_record
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE payment_record_2025_02 PARTITION OF payment_record
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE payment_record_2025_03 PARTITION OF payment_record
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE INDEX idx_payment_record_tenant ON payment_record(tenant_id, payment_no);
CREATE INDEX idx_payment_record_party ON payment_record(tenant_id, party_type, party_id);
CREATE INDEX idx_payment_record_settlement ON payment_record(tenant_id, settlement_id) WHERE settlement_id IS NOT NULL;
CREATE INDEX idx_payment_record_status ON payment_record(tenant_id, status);
CREATE INDEX idx_payment_record_date ON payment_record(payment_date DESC);

COMMENT ON TABLE payment_record IS '付款记录表（分区）';
COMMENT ON COLUMN payment_record.payment_type IS '付款类型:1-货款,2-运费,3-其他';
COMMENT ON COLUMN payment_record.payment_direction IS '付款方向:1-付款,2-收款';
COMMENT ON COLUMN payment_record.payment_method IS '付款方式:1-银行转账,2-支票,3-现金,4-承兑汇票,5-电汇';
COMMENT ON COLUMN payment_record.status IS '状态:0-待支付,1-支付中,2-已完成,3-已失败';

-- ======================================================================
-- 7. 对账记录表 (reconciliation_record)
-- ======================================================================
CREATE TABLE IF NOT EXISTS reconciliation_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    reconciliation_no VARCHAR(128) NOT NULL,

    -- 对账对象
    party_type VARCHAR(32) NOT NULL,
    party_id UUID NOT NULL,
    party_name VARCHAR(256),

    -- 对账周期
    reconciliation_period VARCHAR(32) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    -- 我方数据
    our_total_amount DECIMAL(15, 2) DEFAULT 0,
    our_paid_amount DECIMAL(15, 2) DEFAULT 0,
    our_unpaid_amount DECIMAL(15, 2) DEFAULT 0,

    -- 对方数据
    their_total_amount DECIMAL(15, 2) DEFAULT 0,
    their_paid_amount DECIMAL(15, 2) DEFAULT 0,
    their_unpaid_amount DECIMAL(15, 2) DEFAULT 0,

    -- 差异
    diff_amount DECIMAL(15, 2) DEFAULT 0,
    has_diff BOOLEAN DEFAULT FALSE,
    diff_reason TEXT,

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_reconciliation_status CHECK (status IN (0, 1, 2, 3)),

    -- 对账人
    reconciler_id UUID,
    reconciler_name VARCHAR(128),
    reconciled_at TIMESTAMPTZ,

    -- 确认人
    confirmer_id UUID,
    confirmer_name VARCHAR(128),
    confirmed_at TIMESTAMPTZ,

    -- 附件
    attachments JSONB DEFAULT '[]',

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT uk_tenant_reconciliation_no UNIQUE (tenant_id, reconciliation_no)
);

CREATE INDEX idx_reconciliation_tenant ON reconciliation_record(tenant_id, party_type) WHERE NOT deleted;
CREATE INDEX idx_reconciliation_party ON reconciliation_record(tenant_id, party_id) WHERE NOT deleted;
CREATE INDEX idx_reconciliation_period ON reconciliation_record(tenant_id, reconciliation_period);
CREATE INDEX idx_reconciliation_status ON reconciliation_record(tenant_id, status) WHERE NOT deleted;
CREATE INDEX idx_reconciliation_diff ON reconciliation_record(tenant_id) WHERE has_diff = TRUE AND NOT deleted;

COMMENT ON TABLE reconciliation_record IS '对账记录表';
COMMENT ON COLUMN reconciliation_record.status IS '状态:0-待对账,1-已对账,2-已确认,3-有差异';

-- ======================================================================
-- 8. 平台服务费表 (platform_service_fee) - SaaS平台专用
-- ======================================================================
CREATE TABLE IF NOT EXISTS platform_service_fee (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- 费用类型
    fee_type SMALLINT NOT NULL
        CONSTRAINT chk_platform_fee_type CHECK (fee_type IN (1, 2, 3, 4)),

    -- 计费周期
    billing_period VARCHAR(32) NOT NULL,            -- 2025-01
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    -- 业务量
    order_count INT DEFAULT 0,
    transaction_amount DECIMAL(15, 2) DEFAULT 0,
    storage_used_gb DECIMAL(10, 2) DEFAULT 0,
    api_calls INT DEFAULT 0,

    -- 费率
    fee_rate DECIMAL(5, 4),                         -- 费率（百分比）
    unit_price DECIMAL(10, 2),                      -- 单价

    -- 计算金额
    base_fee DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_platform_base_fee CHECK (base_fee >= 0),
    transaction_fee DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_platform_transaction_fee CHECK (transaction_fee >= 0),
    storage_fee DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_platform_storage_fee CHECK (storage_fee >= 0),
    api_fee DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_platform_api_fee CHECK (api_fee >= 0),
    total_fee DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_platform_total_fee CHECK (total_fee >= 0),

    -- 优惠
    discount_amount DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_platform_discount CHECK (discount_amount >= 0),
    final_fee DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_platform_final_fee CHECK (final_fee >= 0),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_platform_fee_status CHECK (status IN (0, 1, 2)),

    -- 付款
    paid_amount DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_platform_paid CHECK (paid_amount >= 0),
    paid_at TIMESTAMPTZ,

    -- 发票
    invoice_id UUID,
    invoice_no VARCHAR(128),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT,

    CONSTRAINT uk_tenant_platform_fee_period UNIQUE (tenant_id, billing_period, fee_type)
);

CREATE INDEX idx_platform_fee_tenant ON platform_service_fee(tenant_id, billing_period);
CREATE INDEX idx_platform_fee_status ON platform_service_fee(tenant_id, status);
CREATE INDEX idx_platform_fee_unpaid ON platform_service_fee(tenant_id) WHERE status = 0;

COMMENT ON TABLE platform_service_fee IS '平台服务费表（SaaS平台）';
COMMENT ON COLUMN platform_service_fee.fee_type IS '费用类型:1-交易佣金,2-存储费用,3-API调用费,4-增值服务费';
COMMENT ON COLUMN platform_service_fee.status IS '状态:0-待付款,1-已付款,2-已作废';

-- ======================================================================
-- 视图：应收账款汇总
-- ======================================================================
CREATE OR REPLACE VIEW v_accounts_receivable AS
SELECT
    s.tenant_id,
    s.partner_id AS customer_id,
    s.partner_name AS customer_name,
    COUNT(s.id) AS settlement_count,
    SUM(s.actual_amount) AS total_receivable,
    SUM(s.paid_amount) AS total_paid,
    SUM(s.unpaid_amount) AS total_unpaid,
    MIN(s.end_date) AS earliest_period_end
FROM settlement_order s
WHERE s.settlement_type = 2  -- 销售结算
  AND s.partner_type = 'CUSTOMER'
  AND s.status IN (2, 3)  -- 待付款、部分付款
  AND NOT s.deleted
GROUP BY s.tenant_id, s.partner_id, s.partner_name;

COMMENT ON VIEW v_accounts_receivable IS '应收账款汇总视图';

-- ======================================================================
-- 视图：应付账款汇总
-- ======================================================================
CREATE OR REPLACE VIEW v_accounts_payable AS
SELECT
    s.tenant_id,
    s.partner_id AS supplier_id,
    s.partner_name AS supplier_name,
    COUNT(s.id) AS settlement_count,
    SUM(s.actual_amount) AS total_payable,
    SUM(s.paid_amount) AS total_paid,
    SUM(s.unpaid_amount) AS total_unpaid,
    MIN(s.end_date) AS earliest_period_end
FROM settlement_order s
WHERE s.settlement_type IN (1, 3)  -- 采购结算、物流结算
  AND s.partner_type IN ('SUPPLIER', 'CARRIER')
  AND s.status IN (2, 3)  -- 待付款、部分付款
  AND NOT s.deleted
GROUP BY s.tenant_id, s.partner_id, s.partner_name;

COMMENT ON VIEW v_accounts_payable IS '应付账款汇总视图';

-- ======================================================================
-- 函数：计算运费
-- ======================================================================
CREATE OR REPLACE FUNCTION fn_calculate_freight(
    p_tenant_id UUID,
    p_weight DECIMAL,
    p_province VARCHAR,
    p_city VARCHAR,
    p_order_amount DECIMAL
)
RETURNS JSONB AS $$
DECLARE
    v_rule RECORD;
    v_freight DECIMAL := 0;
    v_detail JSONB;
BEGIN
    -- 查找适用的运费规则（按优先级）
    SELECT * INTO v_rule
    FROM freight_rule
    WHERE tenant_id = p_tenant_id
      AND enabled = TRUE
      AND CURRENT_DATE BETWEEN effective_date AND COALESCE(expiry_date, '9999-12-31')
      AND (regions IS NULL OR p_province = ANY(regions) OR p_city = ANY(regions))
      AND NOT deleted
    ORDER BY priority DESC, create_time DESC
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'NO_RULE_FOUND',
            'message', '未找到适用的运费规则'
        );
    END IF;

    -- 检查是否满足免运费条件
    IF v_rule.free_threshold IS NOT NULL AND p_order_amount >= v_rule.free_threshold THEN
        RETURN jsonb_build_object(
            'success', true,
            'freight', 0,
            'is_free', true,
            'free_reason', '订单金额满' || v_rule.free_threshold || '免运费',
            'rule_id', v_rule.id,
            'rule_code', v_rule.rule_code
        );
    END IF;

    -- 根据计费类型计算运费
    CASE v_rule.billing_type
        WHEN 1 THEN  -- 按重量
            IF p_weight <= v_rule.first_weight THEN
                v_freight := v_rule.first_price;
            ELSE
                v_freight := v_rule.first_price +
                             CEIL((p_weight - v_rule.first_weight) / v_rule.additional_weight) * v_rule.additional_price;
            END IF;
        WHEN 4 THEN  -- 固定运费
            v_freight := v_rule.fixed_freight;
        ELSE
            v_freight := v_rule.first_price;
    END CASE;

    -- 添加附加费用
    v_freight := v_freight + v_rule.remote_area_fee + v_rule.handling_fee;

    RETURN jsonb_build_object(
        'success', true,
        'freight', v_freight,
        'is_free', false,
        'rule_id', v_rule.id,
        'rule_code', v_rule.rule_code,
        'base_freight', v_freight - v_rule.remote_area_fee - v_rule.handling_fee,
        'remote_area_fee', v_rule.remote_area_fee,
        'handling_fee', v_rule.handling_fee
    );
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION fn_calculate_freight IS '计算运费';

-- ======================================================================
-- 注意事项：
-- 1. 结算单生成可以通过定时任务自动化（月度、季度）
-- 2. 发票信息涉及税务合规，需严格管理
-- 3. 付款记录需要审批流程，建议集成工作流引擎
-- 4. 对账需要双方确认，支持线上对账流程
-- 5. 平台服务费计算需要定时任务（按月执行）
-- 6. 财务数据高度敏感，需要严格的权限控制
-- ======================================================================