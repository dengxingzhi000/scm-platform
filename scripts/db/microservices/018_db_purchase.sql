-- ======================================================================
-- 采购服务数据库 (db_purchase)
-- 职责：采购申请、采购计划、询价比价、采购合同、采购订单、采购入库
-- ======================================================================

-- 创建数据库
-- CREATE DATABASE db_purchase WITH ENCODING = 'UTF8';

-- 启用必要扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ======================================================================
-- 1. 采购申请单表 (pur_request) - 业务部门发起的采购需求
-- ======================================================================
CREATE TABLE IF NOT EXISTS pur_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,                        -- 租户ID
    request_no VARCHAR(128) NOT NULL UNIQUE,

    -- 申请信息
    request_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_request_type CHECK (request_type IN (1, 2, 3, 4)),
    priority SMALLINT NOT NULL DEFAULT 2
        CONSTRAINT chk_request_priority CHECK (priority IN (1, 2, 3)),

    -- 申请部门
    dept_id UUID NOT NULL,
    dept_name VARCHAR(256),

    -- 申请人
    requester_id UUID NOT NULL,
    requester_name VARCHAR(128),
    requester_phone VARCHAR(32),

    -- 期望交付时间
    expected_delivery TIMESTAMPTZ NOT NULL,

    -- 用途说明
    purpose TEXT NOT NULL,

    -- 预算
    budget_amount DECIMAL(15, 2)
        CONSTRAINT chk_request_budget CHECK (budget_amount IS NULL OR budget_amount >= 0),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_request_status CHECK (status IN (0, 1, 2, 3, 4, 5)),

    -- 审批流程
    approval_flow_id UUID,
    current_approver_id UUID,
    current_approver_name VARCHAR(128),

    -- 审批时间
    submitted_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    reject_reason TEXT,

    -- 转化为采购单
    converted BOOLEAN DEFAULT FALSE,
    purchase_order_id UUID,
    purchase_order_no VARCHAR(128),

    -- 附件
    attachments JSONB DEFAULT '[]',

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX idx_request_no ON pur_request(request_no) WHERE NOT deleted;
CREATE INDEX idx_request_tenant ON pur_request(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_request_status ON pur_request(status) WHERE NOT deleted;
CREATE INDEX idx_request_requester ON pur_request(requester_id) WHERE NOT deleted;
CREATE INDEX idx_request_dept ON pur_request(dept_id) WHERE NOT deleted;
CREATE INDEX idx_request_created ON pur_request(create_time DESC);

COMMENT ON TABLE pur_request IS '采购申请单表';
COMMENT ON COLUMN pur_request.request_type IS '申请类型:1-常规采购,2-紧急采购,3-补货采购,4-工程采购';
COMMENT ON COLUMN pur_request.priority IS '优先级:1-紧急,2-普通,3-低';
COMMENT ON COLUMN pur_request.status IS '状态:0-草稿,1-待审批,2-已审批,3-已驳回,4-已转采购单,5-已关闭';

-- ======================================================================
-- 2. 采购申请明细表 (pur_request_item)
-- ======================================================================
CREATE TABLE IF NOT EXISTS pur_request_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    request_id UUID NOT NULL,
    request_no VARCHAR(128) NOT NULL,

    -- 物料/商品
    material_code VARCHAR(128),
    material_name VARCHAR(256) NOT NULL,
    material_spec VARCHAR(512),                     -- 规格型号
    material_category VARCHAR(128),

    -- 数量
    quantity DECIMAL(12, 3) NOT NULL
        CONSTRAINT chk_request_item_quantity CHECK (quantity > 0),
    unit VARCHAR(32) NOT NULL,                      -- 单位

    -- 预算价格
    budget_price DECIMAL(12, 2)
        CONSTRAINT chk_request_item_budget CHECK (budget_price IS NULL OR budget_price >= 0),

    -- 需求说明
    requirement_desc TEXT,
    quality_standard VARCHAR(256),

    -- 推荐供应商
    suggested_supplier_id UUID,
    suggested_supplier_name VARCHAR(256),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT
);

CREATE INDEX idx_request_item_request ON pur_request_item(request_id);
CREATE INDEX idx_request_item_tenant ON pur_request_item(tenant_id);
CREATE INDEX idx_request_item_material ON pur_request_item(material_code);

COMMENT ON TABLE pur_request_item IS '采购申请明细表';

-- ======================================================================
-- 3. 采购计划表 (pur_plan) - MRP物料需求计划
-- ======================================================================
CREATE TABLE IF NOT EXISTS pur_plan (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plan_no VARCHAR(128) NOT NULL UNIQUE,
    plan_name VARCHAR(256) NOT NULL,

    -- 计划类型
    plan_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_plan_type CHECK (plan_type IN (1, 2, 3)),

    -- 计划周期
    plan_period VARCHAR(32) NOT NULL,               -- 2025Q1, 2025-01
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    -- 预算
    total_budget DECIMAL(15, 2)
        CONSTRAINT chk_plan_budget CHECK (total_budget IS NULL OR total_budget >= 0),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_plan_status CHECK (status IN (0, 1, 2, 3)),

    -- 审批
    approved_by UUID,
    approved_by_name VARCHAR(128),
    approved_at TIMESTAMPTZ,

    -- 执行情况
    execution_rate DECIMAL(5, 2)
        CONSTRAINT chk_plan_execution CHECK (execution_rate IS NULL OR (execution_rate >= 0 AND execution_rate <= 100)),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX idx_plan_no ON pur_plan(plan_no) WHERE NOT deleted;
CREATE INDEX idx_plan_tenant ON pur_plan(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_plan_status ON pur_plan(status) WHERE NOT deleted;
CREATE INDEX idx_plan_period ON pur_plan(plan_period);

COMMENT ON TABLE pur_plan IS '采购计划表（MRP）';
COMMENT ON COLUMN pur_plan.plan_type IS '计划类型:1-月度计划,2-季度计划,3-年度计划';
COMMENT ON COLUMN pur_plan.status IS '状态:0-编制中,1-待审批,2-执行中,3-已完成';

-- ======================================================================
-- 4. 采购计划明细表 (pur_plan_item)
-- ======================================================================
CREATE TABLE IF NOT EXISTS pur_plan_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    plan_no VARCHAR(128) NOT NULL,

    -- 物料
    material_code VARCHAR(128) NOT NULL,
    material_name VARCHAR(256) NOT NULL,
    material_category VARCHAR(128),

    -- 需求数量
    planned_quantity DECIMAL(12, 3) NOT NULL
        CONSTRAINT chk_plan_item_quantity CHECK (planned_quantity > 0),
    unit VARCHAR(32) NOT NULL,

    -- 已采购数量
    purchased_quantity DECIMAL(12, 3) DEFAULT 0
        CONSTRAINT chk_plan_item_purchased CHECK (purchased_quantity >= 0),

    -- 预算
    budget_price DECIMAL(12, 2),
    budget_amount DECIMAL(15, 2),

    -- 预计交付
    expected_delivery DATE,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT
);

CREATE INDEX idx_plan_item_plan ON pur_plan_item(plan_id);
CREATE INDEX idx_plan_item_tenant ON pur_plan_item(tenant_id);
CREATE INDEX idx_plan_item_material ON pur_plan_item(material_code);

COMMENT ON TABLE pur_plan_item IS '采购计划明细表';

-- ======================================================================
-- 5. 询价单表 (pur_rfq) - Request for Quotation
-- ======================================================================
CREATE TABLE IF NOT EXISTS pur_rfq (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    rfq_no VARCHAR(128) NOT NULL UNIQUE,
    rfq_title VARCHAR(256) NOT NULL,

    -- 关联采购申请
    request_id UUID,
    request_no VARCHAR(128),

    -- 询价类型
    rfq_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_rfq_type CHECK (rfq_type IN (1, 2, 3)),

    -- 询价方式
    inquiry_method SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_inquiry_method CHECK (inquiry_method IN (1, 2, 3)),

    -- 截止时间
    deadline TIMESTAMPTZ NOT NULL,

    -- 报价要求
    quotation_requirement TEXT,
    payment_terms VARCHAR(256),
    delivery_terms VARCHAR(256),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_rfq_status CHECK (status IN (0, 1, 2, 3, 4)),

    -- 发起人
    initiator_id UUID NOT NULL,
    initiator_name VARCHAR(128),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX idx_rfq_no ON pur_rfq(rfq_no) WHERE NOT deleted;
CREATE INDEX idx_rfq_tenant ON pur_rfq(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_rfq_status ON pur_rfq(status) WHERE NOT deleted;
CREATE INDEX idx_rfq_request ON pur_rfq(request_id) WHERE NOT deleted;
CREATE INDEX idx_rfq_deadline ON pur_rfq(deadline);

COMMENT ON TABLE pur_rfq IS '询价单表';
COMMENT ON COLUMN pur_rfq.rfq_type IS '询价类型:1-公开询价,2-邀请询价,3-竞价采购';
COMMENT ON COLUMN pur_rfq.inquiry_method IS '询价方式:1-电话询价,2-邮件询价,3-平台询价';
COMMENT ON COLUMN pur_rfq.status IS '状态:0-草稿,1-已发布,2-报价中,3-已截止,4-已关闭';

-- ======================================================================
-- 6. 询价单明细表 (pur_rfq_item)
-- ======================================================================
CREATE TABLE IF NOT EXISTS pur_rfq_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    rfq_id UUID NOT NULL,
    rfq_no VARCHAR(128) NOT NULL,

    -- 物料
    material_code VARCHAR(128) NOT NULL,
    material_name VARCHAR(256) NOT NULL,
    material_spec VARCHAR(512),

    -- 数量
    quantity DECIMAL(12, 3) NOT NULL
        CONSTRAINT chk_rfq_item_quantity CHECK (quantity > 0),
    unit VARCHAR(32) NOT NULL,

    -- 技术要求
    technical_requirement TEXT,
    quality_standard VARCHAR(256),

    -- 交付要求
    delivery_requirement TEXT,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT
);

CREATE INDEX idx_rfq_item_rfq ON pur_rfq_item(rfq_id);
CREATE INDEX idx_rfq_item_tenant ON pur_rfq_item(tenant_id);
CREATE INDEX idx_rfq_item_material ON pur_rfq_item(material_code);

COMMENT ON TABLE pur_rfq_item IS '询价单明细表';

-- ======================================================================
-- 7. 供应商报价单表 (pur_quotation)
-- ======================================================================
CREATE TABLE IF NOT EXISTS pur_quotation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    quotation_no VARCHAR(128) NOT NULL UNIQUE,

    -- 关联询价单
    rfq_id UUID NOT NULL,
    rfq_no VARCHAR(128) NOT NULL,

    -- 供应商
    supplier_id UUID NOT NULL,
    supplier_name VARCHAR(256) NOT NULL,
    supplier_contact VARCHAR(128),
    supplier_phone VARCHAR(32),
    supplier_email VARCHAR(128),

    -- 报价总额
    total_amount DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_quotation_total CHECK (total_amount >= 0),
    tax_rate DECIMAL(5, 4) DEFAULT 0
        CONSTRAINT chk_quotation_tax_rate CHECK (tax_rate >= 0 AND tax_rate <= 1),

    -- 付款条件
    payment_terms VARCHAR(256),
    payment_method VARCHAR(128),

    -- 交货条件
    delivery_terms VARCHAR(256),
    delivery_period VARCHAR(128),                   -- 交货周期

    -- 质保
    warranty_period VARCHAR(128),                   -- 质保期
    warranty_terms TEXT,

    -- 报价有效期
    valid_from DATE NOT NULL,
    valid_until DATE NOT NULL,

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_quotation_status CHECK (status IN (0, 1, 2, 3)),

    -- 是否中标
    is_selected BOOLEAN DEFAULT FALSE,
    selected_by UUID,
    selected_by_name VARCHAR(128),
    selected_at TIMESTAMPTZ,

    -- 附件
    attachments JSONB DEFAULT '[]',

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX idx_quotation_no ON pur_quotation(quotation_no) WHERE NOT deleted;
CREATE INDEX idx_quotation_tenant ON pur_quotation(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_quotation_rfq ON pur_quotation(rfq_id) WHERE NOT deleted;
CREATE INDEX idx_quotation_supplier ON pur_quotation(supplier_id) WHERE NOT deleted;
CREATE INDEX idx_quotation_status ON pur_quotation(status) WHERE NOT deleted;
CREATE INDEX idx_quotation_selected ON pur_quotation(is_selected) WHERE NOT deleted;

COMMENT ON TABLE pur_quotation IS '供应商报价单表';
COMMENT ON COLUMN pur_quotation.status IS '状态:0-草稿,1-已提交,2-已评审,3-已过期';

-- ======================================================================
-- 8. 供应商报价明细表 (pur_quotation_item)
-- ======================================================================
CREATE TABLE IF NOT EXISTS pur_quotation_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    quotation_id UUID NOT NULL,
    quotation_no VARCHAR(128) NOT NULL,
    rfq_item_id UUID NOT NULL,                      -- 关联询价单明细

    -- 物料
    material_code VARCHAR(128) NOT NULL,
    material_name VARCHAR(256) NOT NULL,
    material_spec VARCHAR(512),

    -- 数量
    quantity DECIMAL(12, 3) NOT NULL
        CONSTRAINT chk_quotation_item_quantity CHECK (quantity > 0),
    unit VARCHAR(32) NOT NULL,

    -- 报价
    unit_price DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_quotation_item_price CHECK (unit_price >= 0),
    tax_rate DECIMAL(5, 4) DEFAULT 0
        CONSTRAINT chk_quotation_item_tax CHECK (tax_rate >= 0 AND tax_rate <= 1),
    subtotal DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_quotation_item_subtotal CHECK (subtotal >= 0),

    -- 交货期
    delivery_days INT
        CONSTRAINT chk_quotation_item_delivery CHECK (delivery_days IS NULL OR delivery_days > 0),

    -- 品牌/产地
    brand VARCHAR(128),
    origin VARCHAR(128),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT
);

CREATE INDEX idx_quotation_item_quotation ON pur_quotation_item(quotation_id);
CREATE INDEX idx_quotation_item_tenant ON pur_quotation_item(tenant_id);
CREATE INDEX idx_quotation_item_rfq ON pur_quotation_item(rfq_item_id);
CREATE INDEX idx_quotation_item_material ON pur_quotation_item(material_code);

COMMENT ON TABLE pur_quotation_item IS '供应商报价明细表';

-- ======================================================================
-- 9. 比价分析表 (pur_price_comparison)
-- ======================================================================
CREATE TABLE IF NOT EXISTS pur_price_comparison (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    comparison_no VARCHAR(128) NOT NULL UNIQUE,

    -- 关联询价单
    rfq_id UUID NOT NULL,
    rfq_no VARCHAR(128) NOT NULL,

    -- 比价维度
    comparison_dimensions JSONB DEFAULT '[]',       -- ["价格", "质量", "交货期", "售后"]

    -- 推荐供应商
    recommended_supplier_id UUID,
    recommended_supplier_name VARCHAR(256),
    recommendation_reason TEXT,

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_comparison_status CHECK (status IN (0, 1, 2)),

    -- 审批
    approved_by UUID,
    approved_by_name VARCHAR(128),
    approved_at TIMESTAMPTZ,

    -- 分析人
    analyst_id UUID,
    analyst_name VARCHAR(128),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX idx_comparison_no ON pur_price_comparison(comparison_no) WHERE NOT deleted;
CREATE INDEX idx_comparison_tenant ON pur_price_comparison(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_comparison_rfq ON pur_price_comparison(rfq_id) WHERE NOT deleted;
CREATE INDEX idx_comparison_status ON pur_price_comparison(status) WHERE NOT deleted;

COMMENT ON TABLE pur_price_comparison IS '比价分析表';
COMMENT ON COLUMN pur_price_comparison.status IS '状态:0-比价中,1-已完成,2-已审批';

-- ======================================================================
-- 10. 比价明细表 (pur_price_comparison_item)
-- ======================================================================
CREATE TABLE IF NOT EXISTS pur_price_comparison_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    comparison_id UUID NOT NULL,
    quotation_id UUID NOT NULL,

    -- 供应商
    supplier_id UUID NOT NULL,
    supplier_name VARCHAR(256) NOT NULL,

    -- 评分（JSON格式存储各维度评分）
    scores JSONB DEFAULT '{}',                      -- {"价格": 90, "质量": 85, ...}
    total_score DECIMAL(5, 2)
        CONSTRAINT chk_comparison_item_score CHECK (total_score IS NULL OR (total_score >= 0 AND total_score <= 100)),

    -- 排名
    rank INT,

    -- 优劣势分析
    advantages TEXT,
    disadvantages TEXT,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT
);

CREATE INDEX idx_comparison_item_comparison ON pur_price_comparison_item(comparison_id);
CREATE INDEX idx_comparison_item_tenant ON pur_price_comparison_item(tenant_id);
CREATE INDEX idx_comparison_item_quotation ON pur_price_comparison_item(quotation_id);
CREATE INDEX idx_comparison_item_supplier ON pur_price_comparison_item(supplier_id);

COMMENT ON TABLE pur_price_comparison_item IS '比价明细表';

-- ======================================================================
-- 11. 采购合同表 (pur_contract) - 长期供货协议
-- ======================================================================
CREATE TABLE IF NOT EXISTS pur_contract (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    contract_no VARCHAR(128) NOT NULL UNIQUE,
    contract_name VARCHAR(256) NOT NULL,

    -- 合同类型
    contract_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_contract_type CHECK (contract_type IN (1, 2, 3)),

    -- 供应商
    supplier_id UUID NOT NULL,
    supplier_name VARCHAR(256) NOT NULL,

    -- 合同金额
    contract_amount DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_contract_amount CHECK (contract_amount >= 0),

    -- 合同期限
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    -- 付款条款
    payment_terms TEXT,

    -- 交货条款
    delivery_terms TEXT,

    -- 质量条款
    quality_terms TEXT,

    -- 违约条款
    penalty_terms TEXT,

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_contract_status CHECK (status IN (0, 1, 2, 3, 4)),

    -- 签署信息
    signed_by UUID,
    signed_by_name VARCHAR(128),
    signed_at TIMESTAMPTZ,

    -- 甲方代表
    party_a_representative VARCHAR(128),
    party_a_contact VARCHAR(128),

    -- 乙方代表（供应商）
    party_b_representative VARCHAR(128),
    party_b_contact VARCHAR(128),

    -- 合同文件
    contract_files JSONB DEFAULT '[]',

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX idx_contract_no ON pur_contract(contract_no) WHERE NOT deleted;
CREATE INDEX idx_contract_tenant ON pur_contract(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_contract_supplier ON pur_contract(supplier_id) WHERE NOT deleted;
CREATE INDEX idx_contract_status ON pur_contract(status) WHERE NOT deleted;
CREATE INDEX idx_contract_period ON pur_contract(start_date, end_date);

COMMENT ON TABLE pur_contract IS '采购合同表';
COMMENT ON COLUMN pur_contract.contract_type IS '合同类型:1-框架协议,2-采购合同,3-补充协议';
COMMENT ON COLUMN pur_contract.status IS '状态:0-草稿,1-待签署,2-执行中,3-已完成,4-已终止';

-- ======================================================================
-- 12. 采购订单表 (pur_order) - 分区表
-- ======================================================================
CREATE TABLE IF NOT EXISTS pur_order (
    id UUID DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    order_no VARCHAR(128) NOT NULL,

    -- 关联
    request_id UUID,                                -- 采购申请ID
    request_no VARCHAR(128),
    rfq_id UUID,                                    -- 询价单ID
    quotation_id UUID,                              -- 报价单ID
    contract_id UUID,                               -- 采购合同ID

    -- 供应商
    supplier_id UUID NOT NULL,
    supplier_name VARCHAR(256) NOT NULL,
    supplier_contact VARCHAR(128),

    -- 订单类型
    order_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_order_type CHECK (order_type IN (1, 2, 3, 4)),

    -- 金额
    total_amount DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_order_total CHECK (total_amount >= 0),
    tax_amount DECIMAL(15, 2) DEFAULT 0
        CONSTRAINT chk_order_tax CHECK (tax_amount >= 0),
    freight_amount DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_order_freight CHECK (freight_amount >= 0),
    discount_amount DECIMAL(15, 2) DEFAULT 0
        CONSTRAINT chk_order_discount CHECK (discount_amount >= 0),
    payable_amount DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_order_payable CHECK (payable_amount >= 0),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_order_status CHECK (status IN (0, 1, 2, 3, 4, 5, 6, 7)),

    -- 收货仓库
    warehouse_id UUID NOT NULL,
    warehouse_name VARCHAR(256),
    warehouse_address TEXT,

    -- 时间
    order_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expected_delivery TIMESTAMPTZ NOT NULL,
    actual_delivery TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancel_reason TEXT,

    -- 采购员
    buyer_id UUID NOT NULL,
    buyer_name VARCHAR(128),
    buyer_phone VARCHAR(32),

    -- 审批
    approval_flow_id UUID,
    approved_by UUID,
    approved_by_name VARCHAR(128),
    approved_at TIMESTAMPTZ,

    -- 付款
    payment_status SMALLINT DEFAULT 0
        CONSTRAINT chk_order_payment CHECK (payment_status IN (0, 1, 2)),
    paid_amount DECIMAL(15, 2) DEFAULT 0
        CONSTRAINT chk_order_paid CHECK (paid_amount >= 0),
    payment_records JSONB DEFAULT '[]',             -- 付款记录数组

    -- 收货
    receipt_status SMALLINT DEFAULT 0
        CONSTRAINT chk_order_receipt CHECK (receipt_status IN (0, 1, 2)),
    receipt_records JSONB DEFAULT '[]',             -- 入库记录数组

    -- 质检
    quality_status SMALLINT DEFAULT 0
        CONSTRAINT chk_order_quality CHECK (quality_status IN (0, 1, 2, 3)),
    quality_report_id UUID,

    -- 发票
    invoice_required BOOLEAN DEFAULT TRUE,
    invoice_status SMALLINT DEFAULT 0
        CONSTRAINT chk_order_invoice CHECK (invoice_status IN (0, 1, 2)),
    invoice_records JSONB DEFAULT '[]',

    -- 附件
    attachments JSONB DEFAULT '[]',

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    -- UNIQUE constraint must include partition key for partitioned tables
    CONSTRAINT uk_pur_order_no_create_time UNIQUE (order_no, create_time)
) PARTITION BY RANGE (create_time);

-- 创建分区（按月分区）
CREATE TABLE pur_order_2025_01 PARTITION OF pur_order
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE pur_order_2025_02 PARTITION OF pur_order
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE pur_order_2025_03 PARTITION OF pur_order
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE TABLE pur_order_2025_04 PARTITION OF pur_order
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');

CREATE TABLE pur_order_2025_05 PARTITION OF pur_order
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');

CREATE TABLE pur_order_2025_06 PARTITION OF pur_order
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');

CREATE TABLE pur_order_2025_07 PARTITION OF pur_order
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');

CREATE TABLE pur_order_2025_08 PARTITION OF pur_order
    FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');

CREATE TABLE pur_order_2025_09 PARTITION OF pur_order
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');

CREATE TABLE pur_order_2025_10 PARTITION OF pur_order
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');

CREATE TABLE pur_order_2025_11 PARTITION OF pur_order
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

CREATE TABLE pur_order_2025_12 PARTITION OF pur_order
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- 索引
CREATE INDEX idx_order_no ON pur_order(order_no);
CREATE INDEX idx_order_tenant ON pur_order(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_order_supplier ON pur_order(supplier_id) WHERE NOT deleted;
CREATE INDEX idx_order_status ON pur_order(status) WHERE NOT deleted;
CREATE INDEX idx_order_buyer ON pur_order(buyer_id) WHERE NOT deleted;
CREATE INDEX idx_order_request ON pur_order(request_id) WHERE NOT deleted;
CREATE INDEX idx_order_warehouse ON pur_order(warehouse_id) WHERE NOT deleted;
CREATE INDEX idx_order_created ON pur_order(create_time DESC);

COMMENT ON TABLE pur_order IS '采购订单表（分区）';
COMMENT ON COLUMN pur_order.order_type IS '订单类型:1-标准采购,2-紧急采购,3-补货采购,4-工程采购';
COMMENT ON COLUMN pur_order.status IS '状态:0-草稿,1-待审批,2-已审批,3-已下单,4-部分收货,5-已完成,6-已取消,7-异常';
COMMENT ON COLUMN pur_order.payment_status IS '付款状态:0-未付款,1-部分付款,2-已付款';
COMMENT ON COLUMN pur_order.receipt_status IS '收货状态:0-未收货,1-部分收货,2-已收货';
COMMENT ON COLUMN pur_order.quality_status IS '质检状态:0-待质检,1-质检合格,2-质检不合格,3-免检';
COMMENT ON COLUMN pur_order.invoice_status IS '发票状态:0-未开票,1-部分开票,2-已开票';

-- ======================================================================
-- 13. 采购订单明细表 (pur_order_item)
-- ======================================================================
CREATE TABLE IF NOT EXISTS pur_order_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    order_no VARCHAR(128) NOT NULL,

    -- 物料/SKU
    material_code VARCHAR(128) NOT NULL,
    material_name VARCHAR(256) NOT NULL,
    material_spec VARCHAR(512),
    material_category VARCHAR(128),

    -- SKU（如果是成品）
    sku_id UUID,
    sku_code VARCHAR(128),

    -- 数量
    quantity DECIMAL(12, 3) NOT NULL
        CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
    unit VARCHAR(32) NOT NULL,

    -- 已收货数量
    received_quantity DECIMAL(12, 3) DEFAULT 0
        CONSTRAINT chk_order_item_received CHECK (received_quantity >= 0),

    -- 合格数量
    qualified_quantity DECIMAL(12, 3) DEFAULT 0
        CONSTRAINT chk_order_item_qualified CHECK (qualified_quantity >= 0),

    -- 价格
    unit_price DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_order_item_price CHECK (unit_price >= 0),
    tax_rate DECIMAL(5, 4) DEFAULT 0
        CONSTRAINT chk_order_item_tax CHECK (tax_rate >= 0 AND tax_rate <= 1),
    discount_rate DECIMAL(5, 4) DEFAULT 0
        CONSTRAINT chk_order_item_discount CHECK (discount_rate >= 0 AND discount_rate <= 1),
    subtotal DECIMAL(15, 2) NOT NULL
        CONSTRAINT chk_order_item_subtotal CHECK (subtotal >= 0),

    -- 交货期
    delivery_date DATE,

    -- 质量要求
    quality_requirement TEXT,

    -- 备注
    item_remark TEXT,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_item_order ON pur_order_item(order_id);
CREATE INDEX idx_order_item_tenant ON pur_order_item(tenant_id);
CREATE INDEX idx_order_item_material ON pur_order_item(material_code);
CREATE INDEX idx_order_item_sku ON pur_order_item(sku_id);
CREATE INDEX idx_order_item_order_no ON pur_order_item(order_no);

COMMENT ON TABLE pur_order_item IS '采购订单明细表';

-- ======================================================================
-- 14. 采购入库单表 (pur_receipt)
-- ======================================================================
CREATE TABLE IF NOT EXISTS pur_receipt (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    receipt_no VARCHAR(128) NOT NULL UNIQUE,

    -- 关联采购订单
    order_id UUID NOT NULL,
    order_no VARCHAR(128) NOT NULL,

    -- 供应商
    supplier_id UUID NOT NULL,
    supplier_name VARCHAR(256) NOT NULL,

    -- 仓库
    warehouse_id UUID NOT NULL,
    warehouse_name VARCHAR(256),

    -- 收货类型
    receipt_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_receipt_type CHECK (receipt_type IN (1, 2, 3)),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_receipt_status CHECK (status IN (0, 1, 2, 3)),

    -- 收货人
    receiver_id UUID NOT NULL,
    receiver_name VARCHAR(128),
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- 质检
    quality_inspector_id UUID,
    quality_inspector_name VARCHAR(128),
    quality_inspected_at TIMESTAMPTZ,
    quality_result SMALLINT
        CONSTRAINT chk_receipt_quality CHECK (quality_result IS NULL OR quality_result IN (1, 2, 3)),
    quality_remark TEXT,

    -- 上架
    shelved BOOLEAN DEFAULT FALSE,
    shelved_by UUID,
    shelved_by_name VARCHAR(128),
    shelved_at TIMESTAMPTZ,

    -- 附件（送货单、质检报告等）
    attachments JSONB DEFAULT '[]',

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX idx_receipt_no ON pur_receipt(receipt_no) WHERE NOT deleted;
CREATE INDEX idx_receipt_tenant ON pur_receipt(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_receipt_order ON pur_receipt(order_id) WHERE NOT deleted;
CREATE INDEX idx_receipt_supplier ON pur_receipt(supplier_id) WHERE NOT deleted;
CREATE INDEX idx_receipt_warehouse ON pur_receipt(warehouse_id) WHERE NOT deleted;
CREATE INDEX idx_receipt_status ON pur_receipt(status) WHERE NOT deleted;
CREATE INDEX idx_receipt_received ON pur_receipt(received_at DESC);

COMMENT ON TABLE pur_receipt IS '采购入库单表';
COMMENT ON COLUMN pur_receipt.receipt_type IS '收货类型:1-正常收货,2-退货,3-换货';
COMMENT ON COLUMN pur_receipt.status IS '状态:0-待收货,1-已收货,2-已质检,3-已入库';
COMMENT ON COLUMN pur_receipt.quality_result IS '质检结果:1-合格,2-不合格,3-部分合格';

-- ======================================================================
-- 15. 采购入库明细表 (pur_receipt_item)
-- ======================================================================
CREATE TABLE IF NOT EXISTS pur_receipt_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    receipt_id UUID NOT NULL,
    receipt_no VARCHAR(128) NOT NULL,
    order_item_id UUID NOT NULL,                    -- 关联采购订单明细

    -- 物料
    material_code VARCHAR(128) NOT NULL,
    material_name VARCHAR(256) NOT NULL,

    -- 应收数量
    expected_quantity DECIMAL(12, 3) NOT NULL
        CONSTRAINT chk_receipt_item_expected CHECK (expected_quantity > 0),

    -- 实收数量
    actual_quantity DECIMAL(12, 3) NOT NULL
        CONSTRAINT chk_receipt_item_actual CHECK (actual_quantity >= 0),

    -- 合格数量
    qualified_quantity DECIMAL(12, 3) DEFAULT 0
        CONSTRAINT chk_receipt_item_qualified CHECK (qualified_quantity >= 0),

    -- 不合格数量
    unqualified_quantity DECIMAL(12, 3) DEFAULT 0
        CONSTRAINT chk_receipt_item_unqualified CHECK (unqualified_quantity >= 0),

    unit VARCHAR(32) NOT NULL,

    -- 批次号
    batch_no VARCHAR(128),

    -- 生产日期/有效期
    production_date DATE,
    expiry_date DATE,

    -- 质检结果
    quality_result SMALLINT
        CONSTRAINT chk_receipt_item_quality CHECK (quality_result IS NULL OR quality_result IN (1, 2, 3)),

    -- 存放位置
    location_code VARCHAR(128),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT
);

CREATE INDEX idx_receipt_item_receipt ON pur_receipt_item(receipt_id);
CREATE INDEX idx_receipt_item_tenant ON pur_receipt_item(tenant_id);
CREATE INDEX idx_receipt_item_order ON pur_receipt_item(order_item_id);
CREATE INDEX idx_receipt_item_material ON pur_receipt_item(material_code);
CREATE INDEX idx_receipt_item_batch ON pur_receipt_item(batch_no);

COMMENT ON TABLE pur_receipt_item IS '采购入库明细表';
COMMENT ON COLUMN pur_receipt_item.quality_result IS '质检结果:1-合格,2-不合格,3-待定';

-- ======================================================================
-- 索引优化建议
-- ======================================================================

-- 1. 采购申请单按月分区，需定期添加新分区
-- 2. 采购订单按月分区，需定期添加新分区
-- 3. 使用 BRIN 索引优化时间范围查询（大表）
-- 4. 考虑使用部分索引过滤已删除记录
-- 5. 定期 ANALYZE 和 VACUUM