-- V1__create_mall_tables.sql
-- Shopping cart
CREATE TABLE mall_cart (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    sku_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    selected BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seller management (marketplace)
CREATE TABLE mall_seller (
    id BIGINT PRIMARY KEY,
    seller_name VARCHAR(128) NOT NULL,
    seller_type VARCHAR(32),
    contact_name VARCHAR(64),
    contact_phone VARCHAR(20),
    license_no VARCHAR(64),
    license_image VARCHAR(256),
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mall_seller_shop (
    id BIGINT PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    shop_name VARCHAR(128) NOT NULL,
    shop_logo VARCHAR(256),
    shop_description TEXT,
    category_ids JSONB,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Product display (denormalized for performance)
CREATE TABLE mall_product_display (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    seller_id BIGINT,
    title VARCHAR(256) NOT NULL,
    subtitle VARCHAR(512),
    main_image VARCHAR(256),
    images_json JSONB,
    price_range VARCHAR(64),
    sales_count INT DEFAULT 0,
    review_count INT DEFAULT 0,
    rating DECIMAL(3,2) DEFAULT 5.00,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Product reviews
CREATE TABLE mall_review (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT,
    order_no VARCHAR(64),
    rating SMALLINT NOT NULL,
    content TEXT,
    images_json JSONB,
    is_anonymous BOOLEAN DEFAULT FALSE,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mall_review_reply (
    id BIGINT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Browsing history
CREATE TABLE mall_browsing_history (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL,
    category_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Favorites
CREATE TABLE mall_favorite (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, product_id)
);

-- Indexes
CREATE INDEX idx_mall_cart_user ON mall_cart(user_id);
CREATE INDEX idx_mall_cart_product ON mall_cart(product_id, sku_id);
CREATE INDEX idx_mall_seller_status ON mall_seller(status);
CREATE INDEX idx_mall_product_display ON mall_product_display(product_id, status);
CREATE INDEX idx_mall_review_product ON mall_review(product_id, status);
CREATE INDEX idx_mall_review_user ON mall_review(user_id);
CREATE INDEX idx_mall_history_user ON mall_browsing_history(user_id, created_at DESC);
CREATE INDEX idx_mall_favorite_user ON mall_favorite(user_id);
