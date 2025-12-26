-- ======================================================================
-- 订单服务数据库 (db_order)
-- 职责：订单管理、订单明细、支付记录、退款、状态流转
-- ======================================================================

-- 创建数据库
-- CREATE DATABASE db_order WITH ENCODING = 'UTF8';

-- 启用必要扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ======================================================================
-- 1. 订单表 (ord_order) - 分区表（按创建时间月份分区）
-- ======================================================================
CREATE TABLE IF NOT EXISTS ord_order (
    id UUID DEFAULT gen_random_uuid(),
    order_no VARCHAR(128) NOT NULL,                -- 订单号

    -- 用户信息
    user_id UUID NOT NULL,                         -- 关联 db_user.sys_user.id
    username VARCHAR(128),

    -- 订单类型
    order_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_order_type CHECK (order_type IN (1, 2, 3, 4)),
    order_source VARCHAR(32) DEFAULT 'WEB',        -- WEB, MOBILE, WECHAT, API

    -- 订单状态（Spring State Machine）
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_order_status CHECK (status IN (0, 1, 2, 3, 4, 5, 6, 7, 8, 9)),

    -- 金额
    total_amount DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_order_total CHECK (total_amount >= 0),
    discount_amount DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_order_discount CHECK (discount_amount >= 0),
    freight_amount DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_order_freight CHECK (freight_amount >= 0),
    payable_amount DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_order_payable CHECK (payable_amount >= 0),

    -- 支付
    payment_method SMALLINT
        CONSTRAINT chk_payment_method CHECK (payment_method IN (1, 2, 3, 4, 5)),
    payment_no VARCHAR(128),
    paid_amount DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_paid_amount CHECK (paid_amount >= 0),
    paid_at TIMESTAMPTZ,

    -- 收货地址（JSONB）
    shipping_address JSONB NOT NULL,
    -- 示例：{"receiverName":"张三","phone":"13800138000","province":"北京市","city":"北京市","district":"朝阳区","detail":"三里屯SOHO A座 1001室"}

    -- 发票
    invoice_required BOOLEAN DEFAULT FALSE,
    invoice_info JSONB,

    -- 仓库和物流
    warehouse_id UUID,
    waybill_no VARCHAR(128),
    carrier VARCHAR(128),
    shipped_at TIMESTAMPTZ,
    estimated_arrival TIMESTAMPTZ,

    -- 时间节点
    payment_deadline TIMESTAMPTZ,
    auto_cancel_at TIMESTAMPTZ,
    auto_complete_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancel_reason TEXT,

    -- 库存预占
    reservation_id UUID,

    -- 备注
    buyer_message TEXT,
    seller_message TEXT,
    tags VARCHAR(64)[],
    extra_data JSONB DEFAULT '{}',

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    -- UNIQUE constraint must include partition key for partitioned tables
    CONSTRAINT uk_order_no_create_time UNIQUE (order_no, create_time)
) PARTITION BY RANGE (create_time);

-- 创建分区（按月分区）
CREATE TABLE ord_order_2025_01 PARTITION OF ord_order
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE ord_order_2025_02 PARTITION OF ord_order
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE ord_order_2025_03 PARTITION OF ord_order
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

-- 索引
CREATE INDEX idx_order_no ON ord_order(order_no);
CREATE INDEX idx_order_user ON ord_order(user_id);
CREATE INDEX idx_order_status ON ord_order(status, create_time DESC);
CREATE INDEX idx_order_payment_no ON ord_order(payment_no) WHERE payment_no IS NOT NULL;
CREATE INDEX idx_order_waybill ON ord_order(waybill_no) WHERE waybill_no IS NOT NULL;
CREATE INDEX idx_order_created ON ord_order(create_time DESC);
CREATE INDEX idx_order_deadline ON ord_order(payment_deadline) WHERE status = 0;
CREATE INDEX idx_order_auto_cancel ON ord_order(auto_cancel_at) WHERE status = 0;

COMMENT ON TABLE ord_order IS '订单表（分区表）';
COMMENT ON COLUMN ord_order.order_type IS '订单类型:1-普通,2-秒杀,3-预售,4-团购';
COMMENT ON COLUMN ord_order.status IS '状态:0-待支付,1-已支付,2-待发货,3-已发货,4-运输中,5-已送达,6-已完成,7-已取消,8-退款中,9-已退款';
COMMENT ON COLUMN ord_order.payment_method IS '支付方式:1-支付宝,2-微信,3-银行卡,4-余额,5-货到付款';

-- ======================================================================
-- 2. 订单明细表 (ord_order_item)
-- ======================================================================
CREATE TABLE IF NOT EXISTS ord_order_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    order_no VARCHAR(128) NOT NULL,

    -- 商品信息
    sku_id UUID NOT NULL,
    spu_id UUID NOT NULL,
    sku_code VARCHAR(128),
    sku_name VARCHAR(256) NOT NULL,
    spu_name VARCHAR(256),

    -- 属性快照（下单时SKU属性）
    attributes JSONB DEFAULT '{}',
    image_url VARCHAR(512),

    -- 价格和数量
    original_price DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_item_original_price CHECK (original_price >= 0),
    selling_price DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_item_selling_price CHECK (selling_price >= 0),
    quantity INT NOT NULL
        CONSTRAINT chk_item_quantity CHECK (quantity > 0),
    subtotal DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_item_subtotal CHECK (subtotal >= 0),

    -- 优惠
    discount_amount DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_item_discount CHECK (discount_amount >= 0),
    final_amount DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_item_final CHECK (final_amount >= 0),

    -- 仓库
    warehouse_id UUID,

    -- 售后
    refund_quantity INT DEFAULT 0
        CONSTRAINT chk_refund_quantity CHECK (refund_quantity >= 0 AND refund_quantity <= quantity),
    refund_amount DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_refund_amount CHECK (refund_amount >= 0),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT
);

CREATE INDEX idx_item_order ON ord_order_item(order_id);
CREATE INDEX idx_item_sku ON ord_order_item(sku_id);
CREATE INDEX idx_item_order_no ON ord_order_item(order_no);

COMMENT ON TABLE ord_order_item IS '订单明细表';
COMMENT ON COLUMN ord_order_item.attributes IS '下单时SKU属性快照';

-- ======================================================================
-- 3. 订单状态历史表 (ord_status_history)
-- ======================================================================
CREATE TABLE IF NOT EXISTS ord_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    order_no VARCHAR(128) NOT NULL,

    -- 状态流转（State Machine）
    from_status SMALLINT,
    to_status SMALLINT NOT NULL,
    event VARCHAR(64),                             -- PAYMENT_SUCCESS, SHIP_OUT, 等

    -- 操作人
    operator_id UUID,
    operator_name VARCHAR(128),
    operator_type VARCHAR(32),                     -- SYSTEM, USER, ADMIN

    -- 详情
    remark TEXT,
    extra_data JSONB DEFAULT '{}',

    -- 时间
    transitioned_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_history_order ON ord_status_history(order_id, transitioned_at DESC);
CREATE INDEX idx_history_order_no ON ord_status_history(order_no);
CREATE INDEX idx_history_status ON ord_status_history(to_status, transitioned_at DESC);

COMMENT ON TABLE ord_status_history IS '订单状态流转历史';
COMMENT ON COLUMN ord_status_history.event IS '状态机事件：PAYMENT_SUCCESS, SHIP_OUT等';

-- ======================================================================
-- 4. 支付记录表 (ord_payment)
-- ======================================================================
CREATE TABLE IF NOT EXISTS ord_payment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_no VARCHAR(128) NOT NULL UNIQUE,      -- 内部支付单号

    -- 订单
    order_id UUID NOT NULL,
    order_no VARCHAR(128) NOT NULL,

    -- 用户
    user_id UUID NOT NULL,

    -- 支付方式
    payment_method SMALLINT NOT NULL
        CONSTRAINT chk_payment_pay_method CHECK (payment_method IN (1, 2, 3, 4, 5)),
    payment_channel VARCHAR(64),                   -- ALIPAY_APP, WECHAT_JSAPI等

    -- 金额
    payment_amount DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_payment_amount CHECK (payment_amount > 0),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_payment_status CHECK (status IN (0, 1, 2, 3, 4, 5, 6)),

    -- 第三方信息
    third_party_no VARCHAR(256),
    third_party_response JSONB,

    -- 时间
    initiated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,

    -- 退款
    refund_amount DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_payment_refund CHECK (refund_amount >= 0 AND refund_amount <= payment_amount),
    refund_reason TEXT,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT
);

CREATE INDEX idx_payment_no ON ord_payment(payment_no);
CREATE INDEX idx_payment_order ON ord_payment(order_id);
CREATE INDEX idx_payment_user ON ord_payment(user_id);
CREATE INDEX idx_payment_status ON ord_payment(status);
CREATE INDEX idx_payment_third_party ON ord_payment(third_party_no) WHERE third_party_no IS NOT NULL;
CREATE INDEX idx_payment_created ON ord_payment(create_time DESC);

COMMENT ON TABLE ord_payment IS '支付记录表';
COMMENT ON COLUMN ord_payment.status IS '状态:0-待支付,1-处理中,2-成功,3-失败,4-退款中,5-已退款,6-已取消';
COMMENT ON COLUMN ord_payment.third_party_response IS '第三方支付网关响应（JSONB）';

-- ======================================================================
-- 5. 退款/退货表 (ord_refund)
-- ======================================================================
CREATE TABLE IF NOT EXISTS ord_refund (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    refund_no VARCHAR(128) NOT NULL UNIQUE,

    -- 原订单
    order_id UUID NOT NULL,
    order_no VARCHAR(128) NOT NULL,

    -- 用户
    user_id UUID NOT NULL,

    -- 退款类型
    refund_type SMALLINT NOT NULL
        CONSTRAINT chk_refund_type CHECK (refund_type IN (1, 2)),
    reason VARCHAR(256) NOT NULL,
    description TEXT,
    evidence_images JSONB DEFAULT '[]',            -- 图片URL数组

    -- 退款商品
    refund_items JSONB NOT NULL,                   -- [{skuId, quantity, amount}]

    -- 金额
    refund_amount DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_refund_refund_amount CHECK (refund_amount > 0),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_refund_status CHECK (status IN (0, 1, 2, 3)),
    approved_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    -- 处理人
    handler_id UUID,
    handler_name VARCHAR(128),
    handler_remark TEXT,

    -- 退货物流
    return_waybill_no VARCHAR(128),
    return_carrier VARCHAR(128),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT
);

CREATE INDEX idx_refund_no ON ord_refund(refund_no);
CREATE INDEX idx_refund_order ON ord_refund(order_id);
CREATE INDEX idx_refund_user ON ord_refund(user_id);
CREATE INDEX idx_refund_status ON ord_refund(status, create_time DESC);

COMMENT ON TABLE ord_refund IS '退款/退货表';
COMMENT ON COLUMN ord_refund.refund_type IS '退款类型:1-仅退款,2-退货退款';
COMMENT ON COLUMN ord_refund.status IS '状态:0-待审核,1-已同意,2-已拒绝,3-已完成';

-- ======================================================================
-- 函数：生成订单号
-- ======================================================================
CREATE OR REPLACE FUNCTION fn_generate_order_no()
RETURNS VARCHAR AS $$
BEGIN
    RETURN 'ORD' || TO_CHAR(NOW(), 'YYYYMMDDHH24MISS') ||
           LPAD(FLOOR(RANDOM() * 1000000)::TEXT, 6, '0');
END;
$$ LANGUAGE plpgsql VOLATILE;

COMMENT ON FUNCTION fn_generate_order_no IS '生成订单号: ORD + 时间戳 + 6位随机数';

-- ======================================================================
-- 函数：自动记录状态变更
-- ======================================================================
CREATE OR REPLACE FUNCTION fn_record_order_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status != OLD.status THEN
        INSERT INTO ord_status_history (
            order_id, order_no, from_status, to_status, operator_id
        ) VALUES (
            NEW.id, NEW.order_no, OLD.status, NEW.status, NEW.update_by
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 触发器：自动记录状态变更
CREATE TRIGGER trg_order_status_history
    AFTER UPDATE ON ord_order
    FOR EACH ROW
    WHEN (NEW.status IS DISTINCT FROM OLD.status)
    EXECUTE FUNCTION fn_record_order_status_change();

COMMENT ON FUNCTION fn_record_order_status_change IS '自动记录订单状态变更到历史表';

-- ======================================================================
-- 视图：订单摘要（含明细统计）
-- ======================================================================
CREATE OR REPLACE VIEW v_order_summary AS
SELECT
    o.id,
    o.order_no,
    o.user_id,
    o.status,
    o.order_type,
    o.total_amount,
    o.payable_amount,
    o.payment_method,
    COUNT(i.id) AS item_count,
    SUM(i.quantity) AS total_quantity,
    o.shipping_address->>'receiverName' AS receiver_name,
    o.shipping_address->>'phone' AS receiver_phone,
    o.create_time,
    o.paid_at,
    o.shipped_at,
    o.completed_at
FROM ord_order o
LEFT JOIN ord_order_item i ON o.id = i.order_id
WHERE NOT o.deleted
GROUP BY
    o.id,
    o.order_no,
    o.user_id,
    o.status,
    o.order_type,
    o.total_amount,
    o.payable_amount,
    o.payment_method,
    o.shipping_address,
    o.create_time,
    o.paid_at,
    o.shipped_at,
    o.completed_at;

COMMENT ON VIEW v_order_summary IS '订单摘要视图（含明细统计）';

-- ======================================================================
-- 视图：待支付订单（用于超时取消任务）
-- ======================================================================
CREATE OR REPLACE VIEW v_pending_payment_orders AS
SELECT
    id,
    order_no,
    user_id,
    payment_deadline,
    auto_cancel_at,
    EXTRACT(EPOCH FROM (auto_cancel_at - NOW()))::INT AS seconds_to_cancel
FROM ord_order
WHERE status = 0
  AND auto_cancel_at IS NOT NULL
  AND auto_cancel_at > NOW()
  AND NOT deleted
ORDER BY auto_cancel_at ASC;

COMMENT ON VIEW v_pending_payment_orders IS '待支付订单（用于XXL-Job超时取消任务）';

-- ======================================================================
-- 注意：
-- 1. 订单状态流转使用 Spring State Machine
-- 2. 分区表需定期添加新分区（每月）
-- 3. update_time 由 MyBatis-Plus 自动填充
-- ======================================================================