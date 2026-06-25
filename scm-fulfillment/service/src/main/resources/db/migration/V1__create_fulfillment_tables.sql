-- V1__create_fulfillment_tables.sql
-- Fulfillment orders
CREATE TABLE ful_fulfillment_order (
    id BIGINT PRIMARY KEY,
    fulfillment_no VARCHAR(64) NOT NULL UNIQUE,
    order_no VARCHAR(64) NOT NULL,
    user_id VARCHAR(64),
    status SMALLINT DEFAULT 1,
    fulfillment_type VARCHAR(32),
    warehouse_id BIGINT,
    shipping_method VARCHAR(32),
    receiver_name VARCHAR(64),
    receiver_phone VARCHAR(20),
    receiver_address VARCHAR(512),
    total_items INT,
    total_weight DECIMAL(10,2),
    shipping_fee DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Fulfillment items
CREATE TABLE ful_fulfillment_item (
    id BIGINT PRIMARY KEY,
    fulfillment_no VARCHAR(64) NOT NULL,
    order_item_id BIGINT,
    sku_id BIGINT NOT NULL,
    product_name VARCHAR(256),
    quantity INT NOT NULL,
    picked_quantity INT DEFAULT 0,
    packed_quantity INT DEFAULT 0,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3PL configuration
CREATE TABLE ful_3pl_config (
    id BIGINT PRIMARY KEY,
    provider_name VARCHAR(64) NOT NULL,
    provider_type VARCHAR(32),
    api_url VARCHAR(256),
    api_key VARCHAR(128),
    api_secret VARCHAR(256),
    supported_services JSONB,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ful_3pl_order (
    id BIGINT PRIMARY KEY,
    fulfillment_no VARCHAR(64) NOT NULL,
    provider_id BIGINT,
    provider_order_no VARCHAR(64),
    service_type VARCHAR(32),
    estimated_fee DECIMAL(10,2),
    actual_fee DECIMAL(10,2),
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Real-time tracking
CREATE TABLE ful_tracking (
    id BIGINT PRIMARY KEY,
    fulfillment_no VARCHAR(64) NOT NULL,
    tracking_no VARCHAR(64),
    carrier VARCHAR(32),
    status VARCHAR(32),
    location VARCHAR(128),
    description VARCHAR(256),
    event_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Dropshipping
CREATE TABLE ful_dropship_config (
    id BIGINT PRIMARY KEY,
    supplier_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    min_quantity INT,
    lead_time INT,
    shipping_method VARCHAR(32),
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ful_dropship_order (
    id BIGINT PRIMARY KEY,
    fulfillment_no VARCHAR(64) NOT NULL,
    supplier_id BIGINT,
    supplier_order_no VARCHAR(64),
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Warehouse routing rules
CREATE TABLE ful_routing_rule (
    id BIGINT PRIMARY KEY,
    rule_name VARCHAR(128),
    rule_type VARCHAR(32),
    conditions_json JSONB,
    priority INT DEFAULT 0,
    warehouse_id BIGINT,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_ful_order_no ON ful_fulfillment_order(order_no);
CREATE INDEX idx_ful_fulfillment_no ON ful_fulfillment_order(fulfillment_no);
CREATE INDEX idx_ful_item_fulfillment ON ful_fulfillment_item(fulfillment_no);
CREATE INDEX idx_ful_tracking_no ON ful_tracking(fulfillment_no, event_time DESC);
CREATE INDEX idx_ful_3pl_order ON ful_3pl_order(fulfillment_no);
CREATE INDEX idx_ful_dropship_order ON ful_dropship_order(fulfillment_no);
CREATE INDEX idx_ful_routing_warehouse ON ful_routing_rule(warehouse_id, status);
