-- V1__create_member_tables.sql
-- Member core table
CREATE TABLE mem_member (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    member_no VARCHAR(32) NOT NULL UNIQUE,
    nickname VARCHAR(64),
    avatar VARCHAR(256),
    gender SMALLINT DEFAULT 0,
    birthday DATE,
    member_level INT DEFAULT 1,
    points INT DEFAULT 0,
    total_spent DECIMAL(12,2) DEFAULT 0,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Member levels configuration
CREATE TABLE mem_member_level (
    id BIGINT PRIMARY KEY,
    level_code VARCHAR(32) NOT NULL UNIQUE,
    level_name VARCHAR(64) NOT NULL,
    min_points INT NOT NULL,
    discount_rate DECIMAL(3,2) DEFAULT 1.00,
    privileges_json JSONB,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Address book
CREATE TABLE mem_member_address (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    receiver_name VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(32) NOT NULL,
    city VARCHAR(32) NOT NULL,
    district VARCHAR(32) NOT NULL,
    detail_address VARCHAR(256) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Points transaction log
CREATE TABLE mem_member_points_log (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    points INT NOT NULL,
    type VARCHAR(32) NOT NULL,
    source VARCHAR(64),
    order_no VARCHAR(64),
    description VARCHAR(256),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Third-party bindings
CREATE TABLE mem_third_party_binding (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    open_id VARCHAR(128) NOT NULL,
    union_id VARCHAR(128),
    nickname VARCHAR(64),
    avatar VARCHAR(256),
    binding_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status SMALLINT DEFAULT 1,
    UNIQUE(platform, open_id)
);

-- Member tags
CREATE TABLE mem_member_tag (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    tag_name VARCHAR(64) NOT NULL,
    tag_type VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, tag_name)
);

-- Indexes
CREATE INDEX idx_mem_member_user_id ON mem_member(user_id);
CREATE INDEX idx_mem_address_user_id ON mem_member_address(user_id);
CREATE INDEX idx_mem_points_user_id ON mem_member_points_log(user_id);
CREATE INDEX idx_mem_binding_platform ON mem_third_party_binding(platform, open_id);
CREATE INDEX idx_mem_tag_user ON mem_member_tag(user_id);

-- Seed member levels
INSERT INTO mem_member_level (id, level_code, level_name, min_points, discount_rate, privileges_json) VALUES
(1, 'NORMAL', 'Normal Member', 0, 1.00, '{"freeShipping": false}'),
(2, 'SILVER', 'Silver Member', 1000, 0.98, '{"freeShipping": false, "prioritySupport": true}'),
(3, 'GOLD', 'Gold Member', 5000, 0.95, '{"freeShipping": true, "prioritySupport": true}'),
(4, 'PLATINUM', 'Platinum Member', 20000, 0.90, '{"freeShipping": true, "prioritySupport": true, "exclusiveOffers": true}'),
(5, 'DIAMOND', 'Diamond Member', 50000, 0.85, '{"freeShipping": true, "prioritySupport": true, "exclusiveOffers": true, "personalAdvisor": true}');
