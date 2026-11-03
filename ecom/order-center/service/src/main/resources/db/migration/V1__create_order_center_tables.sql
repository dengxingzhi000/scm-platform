-- V1__create_order_center_tables.sql
-- Unified order (partitioned by create_time)
CREATE TABLE oc_order (
    id BIGINT,
    order_no VARCHAR(64) NOT NULL,
    parent_order_no VARCHAR(64),
    channel VARCHAR(32),
    order_type VARCHAR(32),
    user_id VARCHAR(64) NOT NULL,
    seller_id BIGINT,
    status SMALLINT DEFAULT 1,
    total_amount DECIMAL(12,2),
    discount_amount DECIMAL(12,2),
    payable_amount DECIMAL(12,2),
    paid_amount DECIMAL(12,2),
    shipping_fee DECIMAL(10,2),
    receiver_name VARCHAR(64),
    receiver_phone VARCHAR(20),
    receiver_address VARCHAR(512),
    remark VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Order items
CREATE TABLE oc_order_item (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    product_name VARCHAR(256),
    sku_name VARCHAR(256),
    price DECIMAL(10,2),
    quantity INT,
    discount_amount DECIMAL(10,2),
    payable_amount DECIMAL(10,2),
    seller_id BIGINT,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order payment
CREATE TABLE oc_order_payment (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    payment_no VARCHAR(64),
    payment_method VARCHAR(32),
    amount DECIMAL(12,2),
    status SMALLINT DEFAULT 1,
    paid_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order logistics
CREATE TABLE oc_order_logistics (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    logistics_no VARCHAR(64),
    carrier VARCHAR(32),
    tracking_no VARCHAR(64),
    status SMALLINT DEFAULT 1,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order state machine history
CREATE TABLE oc_order_state_history (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    from_state VARCHAR(32),
    to_state VARCHAR(32) NOT NULL,
    operator VARCHAR(64),
    reason VARCHAR(256),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order events (event sourcing)
CREATE TABLE oc_order_event (
    id BIGINT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    order_no VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_data_json JSONB,
    version INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order split record
CREATE TABLE oc_order_split (
    id BIGINT PRIMARY KEY,
    parent_order_no VARCHAR(64) NOT NULL,
    child_order_no VARCHAR(64) NOT NULL,
    split_type VARCHAR(32),
    split_reason VARCHAR(256),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_oc_order_user ON oc_order(user_id, created_at DESC);
CREATE INDEX idx_oc_order_seller ON oc_order(seller_id, status);
CREATE INDEX idx_oc_order_status ON oc_order(status, created_at DESC);
CREATE INDEX idx_oc_order_no ON oc_order(order_no);
CREATE INDEX idx_oc_order_item_order ON oc_order_item(order_no);
CREATE INDEX idx_oc_order_payment_order ON oc_order_payment(order_no);
CREATE INDEX idx_oc_order_logistics_order ON oc_order_logistics(order_no);
CREATE INDEX idx_oc_event_order ON oc_order_event(order_no, version);
CREATE INDEX idx_oc_split_parent ON oc_order_split(parent_order_no);
