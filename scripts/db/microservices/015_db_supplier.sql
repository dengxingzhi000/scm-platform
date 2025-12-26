-- ======================================================================
-- 供应商服务数据库 (db_supplier)
-- 职责：供应商管理、采购单、供应商评价、对账结算
-- ======================================================================

-- 创建数据库
-- CREATE DATABASE db_supplier WITH ENCODING = 'UTF8';

-- 启用必要扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ======================================================================
-- 1. 供应商表 (sup_supplier)
-- ======================================================================
CREATE TABLE IF NOT EXISTS sup_supplier (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_code VARCHAR(64) NOT NULL UNIQUE,
    supplier_name VARCHAR(256) NOT NULL,
    supplier_name_en VARCHAR(256),

    -- 类型
    supplier_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_supplier_type CHECK (supplier_type IN (1, 2, 3, 4)),
    business_type VARCHAR(64)[],                    -- 经营类型数组

    -- 企业信息
    legal_person VARCHAR(128),
    registered_capital DECIMAL(15, 2),
    registration_no VARCHAR(64),                    -- 工商注册号
    tax_no VARCHAR(64),                             -- 税号
    establishment_date DATE,

    -- 联系方式
    contact_name VARCHAR(128),
    contact_phone VARCHAR(32),
    contact_email VARCHAR(128),
    contact_address TEXT,

    -- 银行信息
    bank_name VARCHAR(128),
    bank_account VARCHAR(64),
    bank_account_name VARCHAR(128),

    -- 评级
    credit_rating VARCHAR(2)
        CONSTRAINT chk_credit_rating CHECK (credit_rating IS NULL OR credit_rating IN ('A+', 'A', 'B+', 'B', 'C', 'D')),
    quality_score DECIMAL(3, 1)
        CONSTRAINT chk_quality_score CHECK (quality_score IS NULL OR (quality_score >= 0 AND quality_score <= 100)),
    delivery_score DECIMAL(3, 1)
        CONSTRAINT chk_delivery_score CHECK (delivery_score IS NULL OR (delivery_score >= 0 AND delivery_score <= 100)),
    service_score DECIMAL(3, 1)
        CONSTRAINT chk_service_score CHECK (service_score IS NULL OR (service_score >= 0 AND service_score <= 100)),

    -- 合作信息
    cooperation_start_date DATE,
    cooperation_status SMALLINT DEFAULT 1
        CONSTRAINT chk_cooperation_status CHECK (cooperation_status IN (0, 1, 2, 3)),

    -- 支付条件
    payment_terms VARCHAR(64),                      -- NET30, NET60, COD
    payment_method VARCHAR(32),                     -- BANK_TRANSFER, CHECK, CASH

    -- 附件
    business_license VARCHAR(512),                  -- 营业执照
    certificates JSONB DEFAULT '[]',                -- 资质证书数组

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

CREATE INDEX idx_supplier_code ON sup_supplier(supplier_code) WHERE NOT deleted;
CREATE INDEX idx_supplier_name ON sup_supplier(supplier_name) WHERE NOT deleted;
CREATE INDEX idx_supplier_enabled ON sup_supplier(enabled) WHERE NOT deleted;
CREATE INDEX idx_supplier_credit ON sup_supplier(credit_rating) WHERE NOT deleted;

COMMENT ON TABLE sup_supplier IS '供应商表';
COMMENT ON COLUMN sup_supplier.supplier_type IS '类型:1-生产商,2-贸易商,3-代理商,4-其他';
COMMENT ON COLUMN sup_supplier.cooperation_status IS '合作状态:0-潜在,1-合作中,2-暂停,3-终止';
COMMENT ON COLUMN sup_supplier.credit_rating IS '信用评级:A+,A,B+,B,C,D';

-- ======================================================================
-- 2. 采购单表 (sup_purchase_order) - 分区表
-- ======================================================================
CREATE TABLE IF NOT EXISTS sup_purchase_order (
    id UUID DEFAULT gen_random_uuid(),
    purchase_no VARCHAR(128) NOT NULL,

    -- 供应商
    supplier_id UUID NOT NULL,
    supplier_name VARCHAR(256),

    -- 采购类型
    purchase_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_purchase_type CHECK (purchase_type IN (1, 2, 3)),

    -- 金额
    total_amount DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_purchase_amount CHECK (total_amount >= 0),
    tax_amount DECIMAL(15, 2) DEFAULT 0
        CONSTRAINT chk_tax_amount CHECK (tax_amount >= 0),
    freight_amount DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_purchase_freight CHECK (freight_amount >= 0),
    payable_amount DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_purchase_payable CHECK (payable_amount >= 0),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_purchase_status CHECK (status IN (0, 1, 2, 3, 4, 5, 6)),

    -- 收货仓库
    warehouse_id UUID,

    -- 时间
    expected_delivery TIMESTAMPTZ,
    actual_delivery TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,

    -- 采购员
    buyer_id UUID,
    buyer_name VARCHAR(128),

    -- 审批
    approver_id UUID,
    approver_name VARCHAR(128),
    approved_at TIMESTAMPTZ,

    -- 付款
    payment_status SMALLINT DEFAULT 0
        CONSTRAINT chk_purchase_payment CHECK (payment_status IN (0, 1, 2)),
    paid_amount DECIMAL(15, 2) DEFAULT 0
        CONSTRAINT chk_purchase_paid CHECK (paid_amount >= 0),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    -- UNIQUE constraint must include partition key for partitioned tables
    CONSTRAINT uk_purchase_no_create_time UNIQUE (purchase_no, create_time),
    CONSTRAINT fk_purchase_supplier FOREIGN KEY (supplier_id) REFERENCES sup_supplier(id)
) PARTITION BY RANGE (create_time);

-- 创建分区（按月分区）
CREATE TABLE sup_purchase_order_2025_01 PARTITION OF sup_purchase_order
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE sup_purchase_order_2025_02 PARTITION OF sup_purchase_order
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE sup_purchase_order_2025_03 PARTITION OF sup_purchase_order
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

-- 索引
CREATE INDEX idx_purchase_no ON sup_purchase_order(purchase_no);
CREATE INDEX idx_purchase_supplier ON sup_purchase_order(supplier_id) WHERE NOT deleted;
CREATE INDEX idx_purchase_status ON sup_purchase_order(status) WHERE NOT deleted;
CREATE INDEX idx_purchase_buyer ON sup_purchase_order(buyer_id) WHERE NOT deleted;
CREATE INDEX idx_purchase_created ON sup_purchase_order(create_time DESC);

COMMENT ON TABLE sup_purchase_order IS '采购单表（分区）';
COMMENT ON COLUMN sup_purchase_order.purchase_type IS '采购类型:1-标准采购,2-紧急采购,3-补货采购';
COMMENT ON COLUMN sup_purchase_order.status IS '状态:0-草稿,1-待审批,2-已审批,3-已发货,4-部分收货,5-已完成,6-已取消';
COMMENT ON COLUMN sup_purchase_order.payment_status IS '付款状态:0-未付款,1-部分付款,2-已付款';

-- ======================================================================
-- 3. 采购单明细表 (sup_purchase_order_item)
-- ======================================================================
CREATE TABLE IF NOT EXISTS sup_purchase_order_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_id UUID NOT NULL,
    purchase_no VARCHAR(128) NOT NULL,

    -- SKU
    sku_id UUID NOT NULL,
    sku_code VARCHAR(128),
    sku_name VARCHAR(256) NOT NULL,

    -- 数量
    quantity INT NOT NULL
        CONSTRAINT chk_purchase_item_quantity CHECK (quantity > 0),
    received_quantity INT DEFAULT 0
        CONSTRAINT chk_purchase_item_received CHECK (received_quantity >= 0),

    -- 价格
    unit_price DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_purchase_item_price CHECK (unit_price >= 0),
    tax_rate DECIMAL(5, 4) DEFAULT 0
        CONSTRAINT chk_purchase_tax_rate CHECK (tax_rate >= 0 AND tax_rate <= 1),
    subtotal DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_purchase_item_subtotal CHECK (subtotal >= 0),

    -- 质量要求
    quality_requirement TEXT,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT
);

CREATE INDEX idx_purchase_item_purchase ON sup_purchase_order_item(purchase_id);
CREATE INDEX idx_purchase_item_sku ON sup_purchase_order_item(sku_id);
CREATE INDEX idx_purchase_item_purchase_no ON sup_purchase_order_item(purchase_no);

COMMENT ON TABLE sup_purchase_order_item IS '采购单明细表';

-- ======================================================================
-- 4. 供应商评价表 (sup_supplier_evaluation)
-- ======================================================================
CREATE TABLE IF NOT EXISTS sup_supplier_evaluation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID NOT NULL,

    -- 评价周期
    evaluation_period VARCHAR(32) NOT NULL,         -- 2025Q1, 2025-01
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    -- 评分
    quality_score DECIMAL(3, 1)
        CONSTRAINT chk_eval_quality CHECK (quality_score >= 0 AND quality_score <= 100),
    delivery_score DECIMAL(3, 1)
        CONSTRAINT chk_eval_delivery CHECK (delivery_score >= 0 AND delivery_score <= 100),
    service_score DECIMAL(3, 1)
        CONSTRAINT chk_eval_service CHECK (service_score >= 0 AND service_score <= 100),
    price_score DECIMAL(3, 1)
        CONSTRAINT chk_eval_price CHECK (price_score >= 0 AND price_score <= 100),
    total_score DECIMAL(3, 1)
        CONSTRAINT chk_eval_total CHECK (total_score >= 0 AND total_score <= 100),

    -- 统计
    purchase_count INT DEFAULT 0,
    total_amount DECIMAL(15, 2) DEFAULT 0,
    on_time_delivery_rate DECIMAL(5, 2),            -- 准时交货率
    quality_pass_rate DECIMAL(5, 2),                -- 质量合格率

    -- 评价人
    evaluator_id UUID,
    evaluator_name VARCHAR(128),
    evaluated_at TIMESTAMPTZ,

    -- 改进建议
    improvement_suggestions TEXT,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    remark TEXT,

    CONSTRAINT fk_evaluation_supplier FOREIGN KEY (supplier_id) REFERENCES sup_supplier(id),
    CONSTRAINT uk_supplier_period UNIQUE (supplier_id, evaluation_period)
);

CREATE INDEX idx_evaluation_supplier ON sup_supplier_evaluation(supplier_id);
CREATE INDEX idx_evaluation_period ON sup_supplier_evaluation(evaluation_period);
CREATE INDEX idx_evaluation_score ON sup_supplier_evaluation(total_score DESC);

COMMENT ON TABLE sup_supplier_evaluation IS '供应商评价表';
COMMENT ON COLUMN sup_supplier_evaluation.evaluation_period IS '评价周期：2025Q1（季度）或2025-01（月度）';

-- ======================================================================
-- 5. 对账单表 (sup_settlement)
-- ======================================================================
CREATE TABLE IF NOT EXISTS sup_settlement (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_no VARCHAR(128) NOT NULL UNIQUE,

    -- 供应商
    supplier_id UUID NOT NULL,
    supplier_name VARCHAR(256),

    -- 对账周期
    settlement_period VARCHAR(32) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    -- 采购单
    purchase_order_ids UUID[],                      -- 包含的采购单ID数组
    purchase_count INT DEFAULT 0,

    -- 金额
    total_amount DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_settlement_total CHECK (total_amount >= 0),
    discount_amount DECIMAL(15, 2) DEFAULT 0
        CONSTRAINT chk_settlement_discount CHECK (discount_amount >= 0),
    actual_amount DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_settlement_actual CHECK (actual_amount >= 0),

    -- 付款
    payment_amount DECIMAL(15, 2) DEFAULT 0
        CONSTRAINT chk_settlement_payment CHECK (payment_amount >= 0),
    payment_date DATE,

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_settlement_status CHECK (status IN (0, 1, 2, 3, 4)),

    -- 审批
    approver_id UUID,
    approver_name VARCHAR(128),
    approved_at TIMESTAMPTZ,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT fk_settlement_supplier FOREIGN KEY (supplier_id) REFERENCES sup_supplier(id)
);

CREATE INDEX idx_settlement_no ON sup_settlement(settlement_no);
CREATE INDEX idx_settlement_supplier ON sup_settlement(supplier_id) WHERE NOT deleted;
CREATE INDEX idx_settlement_period ON sup_settlement(settlement_period);
CREATE INDEX idx_settlement_status ON sup_settlement(status) WHERE NOT deleted;

COMMENT ON TABLE sup_settlement IS '对账单表';
COMMENT ON COLUMN sup_settlement.status IS '状态:0-待确认,1-已确认,2-待付款,3-部分付款,4-已付款';

-- ======================================================================
-- 视图：供应商绩效统计
-- ======================================================================
CREATE OR REPLACE VIEW v_supplier_performance AS
SELECT
    s.id,
    s.supplier_code,
    s.supplier_name,
    s.credit_rating,
    s.quality_score,
    s.delivery_score,
    s.service_score,
    COUNT(DISTINCT po.id) AS purchase_count,
    SUM(po.payable_amount) AS total_purchase_amount,
    AVG(e.total_score) AS avg_evaluation_score,
    s.cooperation_status,
    s.enabled
FROM sup_supplier s
LEFT JOIN sup_purchase_order po ON s.id = po.supplier_id AND NOT po.deleted
LEFT JOIN sup_supplier_evaluation e ON s.id = e.supplier_id
WHERE NOT s.deleted
GROUP BY
    s.id,
    s.supplier_code,
    s.supplier_name,
    s.credit_rating,
    s.quality_score,
    s.delivery_score,
    s.service_score,
    s.cooperation_status,
    s.enabled;

COMMENT ON VIEW v_supplier_performance IS '供应商绩效统计视图';

-- ======================================================================
-- 注意：
-- 1. update_time 由 MyBatis-Plus 自动填充
-- 2. 采购单按月分区，需定期添加新分区
-- 3. 供应商评价可按月度或季度进行
-- 4. 对账单生成可通过定时任务自动化
-- ======================================================================