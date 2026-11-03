-- V1__create_payment_tables.sql
-- Payment orders
CREATE TABLE pay_payment_order (
    id BIGINT PRIMARY KEY,
    payment_no VARCHAR(64) NOT NULL UNIQUE,
    order_no VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'CNY',
    payment_method VARCHAR(32),
    payment_channel VARCHAR(32),
    status SMALLINT DEFAULT 1,
    paid_at TIMESTAMP,
    callback_url VARCHAR(256),
    notify_url VARCHAR(256),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Payment channel configuration
CREATE TABLE pay_channel_config (
    id BIGINT PRIMARY KEY,
    channel_name VARCHAR(32) NOT NULL,
    channel_type VARCHAR(32),
    app_id VARCHAR(128),
    app_secret VARCHAR(256),
    merchant_id VARCHAR(64),
    private_key TEXT,
    public_key TEXT,
    callback_url VARCHAR(256),
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Refund
CREATE TABLE pay_refund (
    id BIGINT PRIMARY KEY,
    refund_no VARCHAR(64) NOT NULL UNIQUE,
    payment_no VARCHAR(64) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    refund_amount DECIMAL(12,2) NOT NULL,
    refund_reason VARCHAR(256),
    status SMALLINT DEFAULT 1,
    refunded_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Reconciliation
CREATE TABLE pay_reconciliation (
    id BIGINT PRIMARY KEY,
    recon_date DATE NOT NULL,
    channel VARCHAR(32) NOT NULL,
    total_count INT,
    total_amount DECIMAL(14,2),
    success_count INT,
    success_amount DECIMAL(14,2),
    diff_count INT,
    diff_amount DECIMAL(14,2),
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_pay_order_no ON pay_payment_order(order_no);
CREATE INDEX idx_pay_user ON pay_payment_order(user_id, status);
CREATE INDEX idx_pay_payment_no ON pay_payment_order(payment_no);
CREATE INDEX idx_pay_refund_no ON pay_refund(payment_no);
CREATE INDEX idx_pay_refund_order ON pay_refund(order_no);
CREATE INDEX idx_pay_recon_date ON pay_reconciliation(recon_date, channel);
