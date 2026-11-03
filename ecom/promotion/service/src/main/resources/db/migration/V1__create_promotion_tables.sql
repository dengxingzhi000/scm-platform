-- V1__create_promotion_tables.sql
-- Promotion activities
CREATE TABLE pro_activity (
    id BIGINT PRIMARY KEY,
    activity_name VARCHAR(128) NOT NULL,
    activity_type VARCHAR(32) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status SMALLINT DEFAULT 1,
    rules_json JSONB,
    applicable_scope VARCHAR(32),
    created_by VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Coupon templates
CREATE TABLE pro_coupon_template (
    id BIGINT PRIMARY KEY,
    template_name VARCHAR(128) NOT NULL,
    coupon_type VARCHAR(32) NOT NULL,
    discount_value DECIMAL(10,2),
    min_amount DECIMAL(10,2),
    max_discount DECIMAL(10,2),
    total_count INT,
    issued_count INT DEFAULT 0,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Issued coupons
CREATE TABLE pro_coupon (
    id BIGINT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    coupon_code VARCHAR(64) NOT NULL UNIQUE,
    status SMALLINT DEFAULT 1,
    used_at TIMESTAMP,
    order_no VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Flash sale
CREATE TABLE pro_flash_sale (
    id BIGINT PRIMARY KEY,
    activity_id BIGINT,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    original_price DECIMAL(10,2),
    flash_price DECIMAL(10,2) NOT NULL,
    flash_stock INT NOT NULL,
    sold_count INT DEFAULT 0,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status SMALLINT DEFAULT 1
);

-- Group buy
CREATE TABLE pro_group_buy (
    id BIGINT PRIMARY KEY,
    activity_id BIGINT,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    group_price DECIMAL(10,2) NOT NULL,
    group_size INT NOT NULL,
    current_size INT DEFAULT 0,
    expire_time TIMESTAMP,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pro_group_buy_member (
    id BIGINT PRIMARY KEY,
    group_buy_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    is_leader BOOLEAN DEFAULT FALSE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bundle deals
CREATE TABLE pro_bundle (
    id BIGINT PRIMARY KEY,
    bundle_name VARCHAR(128),
    bundle_type VARCHAR(32),
    discount_type VARCHAR(32),
    discount_value DECIMAL(10,2),
    min_quantity INT DEFAULT 2,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pro_bundle_item (
    id BIGINT PRIMARY KEY,
    bundle_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT,
    quantity INT DEFAULT 1,
    is_required BOOLEAN DEFAULT TRUE
);

-- Tiered pricing
CREATE TABLE pro_tiered_pricing (
    id BIGINT PRIMARY KEY,
    activity_id BIGINT,
    product_id BIGINT NOT NULL,
    min_quantity INT NOT NULL,
    max_quantity INT,
    unit_price DECIMAL(10,2),
    discount_rate DECIMAL(3,2)
);

-- AI recommendation log
CREATE TABLE pro_recommendation_log (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64),
    product_id BIGINT,
    recommendation_type VARCHAR(32),
    score DECIMAL(5,4),
    position INT,
    clicked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- A/B testing
CREATE TABLE pro_ab_test (
    id BIGINT PRIMARY KEY,
    test_name VARCHAR(128),
    test_type VARCHAR(32),
    variants_json JSONB,
    traffic_split JSONB,
    status SMALLINT DEFAULT 1,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pro_ab_test_assignment (
    id BIGINT PRIMARY KEY,
    test_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    variant VARCHAR(32),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(test_id, user_id)
);

-- Indexes
CREATE INDEX idx_pro_coupon_user ON pro_coupon(user_id, status);
CREATE INDEX idx_pro_coupon_template ON pro_coupon_template(status, start_time, end_time);
CREATE INDEX idx_pro_flash_sale_time ON pro_flash_sale(start_time, end_time);
CREATE INDEX idx_pro_flash_sale_product ON pro_flash_sale(product_id, sku_id);
CREATE INDEX idx_pro_group_buy_activity ON pro_group_buy(activity_id, status);
CREATE INDEX idx_pro_group_buy_member ON pro_group_buy_member(group_buy_id, user_id);
CREATE INDEX idx_pro_recommendation_user ON pro_recommendation_log(user_id);
CREATE INDEX idx_pro_recommendation_product ON pro_recommendation_log(product_id);
CREATE INDEX idx_pro_ab_test_assignment ON pro_ab_test_assignment(test_id, user_id);
