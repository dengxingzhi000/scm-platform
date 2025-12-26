-- ======================================================================
-- 租户服务数据库 (db_tenant)
-- 职责：多租户管理、套餐、资源配额、租户配置、功能开关
-- ======================================================================

-- 创建数据库
-- CREATE DATABASE db_tenant WITH ENCODING = 'UTF8';

-- 启用必要扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ======================================================================
-- 1. 租户表 (tenant)
-- ======================================================================
CREATE TABLE IF NOT EXISTS tenant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_code VARCHAR(64) NOT NULL UNIQUE,
    tenant_name VARCHAR(256) NOT NULL,
    tenant_name_en VARCHAR(256),

    -- 类型
    tenant_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_tenant_type CHECK (tenant_type IN (1, 2, 3)),

    -- 企业信息
    company_name VARCHAR(256),
    legal_person VARCHAR(128),
    registration_no VARCHAR(64),                 -- 统一社会信用代码
    tax_no VARCHAR(64),
    industry VARCHAR(64),                        -- 所属行业
    company_size SMALLINT
        CONSTRAINT chk_company_size CHECK (company_size IS NULL OR company_size IN (1, 2, 3, 4, 5)),

    -- 联系方式
    contact_name VARCHAR(128),
    contact_phone VARCHAR(32),
    contact_email VARCHAR(128),
    address JSONB,                               -- {province, city, district, detail}

    -- 管理员信息
    admin_user_id UUID,                          -- 租户管理员
    admin_username VARCHAR(128),
    admin_email VARCHAR(128),

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_tenant_status CHECK (status IN (0, 1, 2, 3)),

    -- 时间
    trial_start_date DATE,
    trial_end_date DATE,
    contract_start_date DATE,
    contract_end_date DATE,
    activated_at TIMESTAMPTZ,
    suspended_at TIMESTAMPTZ,

    -- 品牌定制
    logo_url VARCHAR(512),
    domain VARCHAR(256),                         -- 自定义域名
    theme_config JSONB DEFAULT '{}',             -- 主题配置

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX idx_tenant_code ON tenant(tenant_code) WHERE NOT deleted;
CREATE INDEX idx_tenant_status ON tenant(status) WHERE NOT deleted;
CREATE INDEX idx_tenant_contract_end ON tenant(contract_end_date) WHERE status = 1;

COMMENT ON TABLE tenant IS '租户表';
COMMENT ON COLUMN tenant.tenant_type IS '租户类型:1-企业版,2-专业版,3-个人版';
COMMENT ON COLUMN tenant.company_size IS '企业规模:1-<50人,2-50-200人,3-200-500人,4-500-2000人,5->2000人';
COMMENT ON COLUMN tenant.status IS '状态:0-试用中,1-正式,2-已暂停,3-已过期';

-- ======================================================================
-- 2. 套餐表 (tenant_package)
-- ======================================================================
CREATE TABLE IF NOT EXISTS tenant_package (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_code VARCHAR(64) NOT NULL UNIQUE,
    package_name VARCHAR(128) NOT NULL,
    package_level SMALLINT NOT NULL
        CONSTRAINT chk_package_level CHECK (package_level IN (1, 2, 3, 4)),

    -- 定价
    price_monthly DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_package_monthly CHECK (price_monthly >= 0),
    price_yearly DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_package_yearly CHECK (price_yearly >= 0),
    discount_rate DECIMAL(5, 4) DEFAULT 0
        CONSTRAINT chk_package_discount CHECK (discount_rate >= 0 AND discount_rate <= 1),

    -- 资源配额（默认值）
    max_users INT DEFAULT 10
        CONSTRAINT chk_package_users CHECK (max_users > 0),
    max_warehouses INT DEFAULT 5
        CONSTRAINT chk_package_warehouses CHECK (max_warehouses > 0),
    max_skus INT DEFAULT 10000
        CONSTRAINT chk_package_skus CHECK (max_skus > 0),
    max_orders_per_day INT DEFAULT 1000
        CONSTRAINT chk_package_orders CHECK (max_orders_per_day > 0),
    max_storage_gb INT DEFAULT 10
        CONSTRAINT chk_package_storage CHECK (max_storage_gb > 0),
    max_api_calls_per_day INT DEFAULT 10000
        CONSTRAINT chk_package_api CHECK (max_api_calls_per_day > 0),

    -- 功能模块（JSONB）
    features JSONB DEFAULT '{}',                 -- {inventory: true, wms: true, oms: true, ...}

    -- 描述
    description TEXT,
    highlights JSONB DEFAULT '[]',               -- 套餐亮点数组

    -- 状态
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_trial BOOLEAN DEFAULT FALSE,              -- 是否试用套餐
    trial_days INT DEFAULT 15,
    sort_order INT DEFAULT 0,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX idx_package_code ON tenant_package(package_code) WHERE NOT deleted;
CREATE INDEX idx_package_enabled ON tenant_package(enabled) WHERE NOT deleted;
CREATE INDEX idx_package_level ON tenant_package(package_level, sort_order);

COMMENT ON TABLE tenant_package IS '租户套餐表';
COMMENT ON COLUMN tenant_package.package_level IS '套餐级别:1-基础版,2-专业版,3-企业版,4-旗舰版';
COMMENT ON COLUMN tenant_package.features IS '功能模块配置（JSONB）';

-- ======================================================================
-- 3. 租户订阅表 (tenant_subscription)
-- ======================================================================
CREATE TABLE IF NOT EXISTS tenant_subscription (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    package_id UUID NOT NULL,

    -- 订阅信息
    subscription_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_subscription_type CHECK (subscription_type IN (1, 2, 3)),
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_subscription_status CHECK (status IN (0, 1, 2, 3)),

    -- 时间
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    auto_renew BOOLEAN DEFAULT TRUE,

    -- 费用
    original_price DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_subscription_original CHECK (original_price >= 0),
    actual_price DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_subscription_actual CHECK (actual_price >= 0),
    discount_amount DECIMAL(12, 2) DEFAULT 0
        CONSTRAINT chk_subscription_discount CHECK (discount_amount >= 0),

    -- 支付
    payment_status SMALLINT DEFAULT 0
        CONSTRAINT chk_subscription_payment CHECK (payment_status IN (0, 1, 2)),
    paid_at TIMESTAMPTZ,
    payment_no VARCHAR(128),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    remark TEXT,

    CONSTRAINT fk_subscription_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_subscription_package FOREIGN KEY (package_id) REFERENCES tenant_package(id)
);

CREATE INDEX idx_subscription_tenant ON tenant_subscription(tenant_id, status);
CREATE INDEX idx_subscription_end_date ON tenant_subscription(end_date) WHERE status = 1;
CREATE INDEX idx_subscription_auto_renew ON tenant_subscription(auto_renew, end_date) WHERE status = 1;

COMMENT ON TABLE tenant_subscription IS '租户订阅表';
COMMENT ON COLUMN tenant_subscription.subscription_type IS '订阅类型:1-月付,2-年付,3-一次性';
COMMENT ON COLUMN tenant_subscription.status IS '状态:0-待支付,1-生效中,2-已过期,3-已取消';
COMMENT ON COLUMN tenant_subscription.payment_status IS '支付状态:0-未支付,1-部分支付,2-已支付';

-- ======================================================================
-- 4. 租户资源配额表 (tenant_resource_quota)
-- ======================================================================
CREATE TABLE IF NOT EXISTS tenant_resource_quota (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- 用户和组织
    max_users INT DEFAULT 10
        CONSTRAINT chk_quota_users CHECK (max_users > 0),
    current_users INT DEFAULT 0
        CONSTRAINT chk_quota_current_users CHECK (current_users >= 0),

    -- 仓库和库位
    max_warehouses INT DEFAULT 5
        CONSTRAINT chk_quota_warehouses CHECK (max_warehouses > 0),
    current_warehouses INT DEFAULT 0
        CONSTRAINT chk_quota_current_warehouses CHECK (current_warehouses >= 0),

    -- 商品
    max_skus INT DEFAULT 10000
        CONSTRAINT chk_quota_skus CHECK (max_skus > 0),
    current_skus INT DEFAULT 0
        CONSTRAINT chk_quota_current_skus CHECK (current_skus >= 0),

    -- 订单
    max_orders_per_day INT DEFAULT 1000
        CONSTRAINT chk_quota_orders CHECK (max_orders_per_day > 0),
    current_orders_today INT DEFAULT 0
        CONSTRAINT chk_quota_current_orders CHECK (current_orders_today >= 0),
    last_order_reset_date DATE DEFAULT CURRENT_DATE,

    -- 存储
    max_storage_gb INT DEFAULT 10
        CONSTRAINT chk_quota_storage CHECK (max_storage_gb > 0),
    current_storage_gb DECIMAL(10, 2) DEFAULT 0
        CONSTRAINT chk_quota_current_storage CHECK (current_storage_gb >= 0),

    -- API调用
    max_api_calls_per_day INT DEFAULT 10000
        CONSTRAINT chk_quota_api CHECK (max_api_calls_per_day > 0),
    current_api_calls_today INT DEFAULT 0
        CONSTRAINT chk_quota_current_api CHECK (current_api_calls_today >= 0),
    last_api_reset_date DATE DEFAULT CURRENT_DATE,

    -- 自定义配额（JSONB扩展）
    custom_quotas JSONB DEFAULT '{}',

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_quota_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uk_quota_tenant UNIQUE (tenant_id)
);

CREATE INDEX idx_quota_tenant ON tenant_resource_quota(tenant_id);
CREATE INDEX idx_quota_orders_exceeded ON tenant_resource_quota(tenant_id) WHERE current_orders_today >= max_orders_per_day;
CREATE INDEX idx_quota_api_exceeded ON tenant_resource_quota(tenant_id) WHERE current_api_calls_today >= max_api_calls_per_day;

COMMENT ON TABLE tenant_resource_quota IS '租户资源配额表';
COMMENT ON COLUMN tenant_resource_quota.last_order_reset_date IS '订单配额重置日期（每日重置）';
COMMENT ON COLUMN tenant_resource_quota.last_api_reset_date IS 'API配额重置日期（每日重置）';

-- ======================================================================
-- 5. 租户配置表 (tenant_config)
-- ======================================================================
CREATE TABLE IF NOT EXISTS tenant_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    config_category VARCHAR(64) NOT NULL,        -- SYSTEM, BUSINESS, UI, NOTIFICATION
    config_key VARCHAR(128) NOT NULL,
    config_value TEXT,
    value_type VARCHAR(32) DEFAULT 'STRING',     -- STRING, NUMBER, BOOLEAN, JSON

    -- 描述
    description TEXT,
    default_value TEXT,

    -- 状态
    is_encrypted BOOLEAN DEFAULT FALSE,           -- 是否加密存储
    is_public BOOLEAN DEFAULT FALSE,              -- 是否可被前端访问

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,

    CONSTRAINT fk_config_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uk_tenant_config UNIQUE (tenant_id, config_category, config_key)
);

CREATE INDEX idx_config_tenant ON tenant_config(tenant_id, config_category);
CREATE INDEX idx_config_public ON tenant_config(tenant_id) WHERE is_public = TRUE;

COMMENT ON TABLE tenant_config IS '租户配置表';
COMMENT ON COLUMN tenant_config.config_category IS '配置分类:SYSTEM-系统,BUSINESS-业务,UI-界面,NOTIFICATION-通知';
COMMENT ON COLUMN tenant_config.is_encrypted IS '敏感配置是否加密（如API密钥）';

-- ======================================================================
-- 6. 租户功能开关表 (tenant_feature)
-- ======================================================================
CREATE TABLE IF NOT EXISTS tenant_feature (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    feature_code VARCHAR(64) NOT NULL,
    feature_name VARCHAR(128) NOT NULL,

    -- 状态
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_beta BOOLEAN DEFAULT FALSE,                -- 是否Beta功能

    -- 限制
    usage_limit INT,                              -- 使用次数限制
    current_usage INT DEFAULT 0,
    expire_at TIMESTAMPTZ,                        -- 功能过期时间

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,

    CONSTRAINT fk_feature_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uk_tenant_feature UNIQUE (tenant_id, feature_code)
);

CREATE INDEX idx_feature_tenant ON tenant_feature(tenant_id, enabled);
CREATE INDEX idx_feature_expire ON tenant_feature(expire_at) WHERE enabled = TRUE AND expire_at IS NOT NULL;

COMMENT ON TABLE tenant_feature IS '租户功能开关表';
COMMENT ON COLUMN tenant_feature.feature_code IS '功能代码:INVENTORY,WMS,OMS,TMS,APS,FINANCE等';
COMMENT ON COLUMN tenant_feature.is_beta IS 'Beta功能标识';

-- ======================================================================
-- 7. 租户操作日志表 (tenant_operation_log) - 分区表
-- ======================================================================
CREATE TABLE IF NOT EXISTS tenant_operation_log (
    id UUID DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- 操作信息
    operation_type VARCHAR(64) NOT NULL,          -- CREATE, UPDATE, DELETE, LOGIN, LOGOUT
    operation_module VARCHAR(64),                 -- USER, PRODUCT, ORDER, INVENTORY
    operation_desc TEXT,

    -- 操作人
    operator_id UUID,
    operator_name VARCHAR(128),
    operator_ip VARCHAR(64),
    user_agent TEXT,

    -- 请求信息
    request_url VARCHAR(512),
    request_method VARCHAR(16),
    request_params JSONB,
    response_status INT,

    -- 执行时间
    execution_time INT,                           -- 毫秒

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (create_time);

-- 创建分区（按月分区）
CREATE TABLE tenant_operation_log_2025_01 PARTITION OF tenant_operation_log
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE tenant_operation_log_2025_02 PARTITION OF tenant_operation_log
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE tenant_operation_log_2025_03 PARTITION OF tenant_operation_log
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE INDEX idx_op_log_tenant ON tenant_operation_log(tenant_id, create_time DESC);
CREATE INDEX idx_op_log_operator ON tenant_operation_log(operator_id, create_time DESC);
CREATE INDEX idx_op_log_type ON tenant_operation_log(operation_type, create_time DESC);

COMMENT ON TABLE tenant_operation_log IS '租户操作日志表（分区）';
COMMENT ON COLUMN tenant_operation_log.execution_time IS '接口执行时长（毫秒）';

-- ======================================================================
-- 视图：租户配额使用情况
-- ======================================================================
CREATE OR REPLACE VIEW v_tenant_quota_usage AS
SELECT
    t.id AS tenant_id,
    t.tenant_code,
    t.tenant_name,
    t.status AS tenant_status,
    q.max_users,
    q.current_users,
    ROUND(q.current_users::DECIMAL / q.max_users * 100, 2) AS users_usage_percent,
    q.max_skus,
    q.current_skus,
    ROUND(q.current_skus::DECIMAL / q.max_skus * 100, 2) AS skus_usage_percent,
    q.max_orders_per_day,
    q.current_orders_today,
    ROUND(q.current_orders_today::DECIMAL / q.max_orders_per_day * 100, 2) AS orders_usage_percent,
    q.max_storage_gb,
    q.current_storage_gb,
    ROUND(q.current_storage_gb / q.max_storage_gb * 100, 2) AS storage_usage_percent,
    q.max_api_calls_per_day,
    q.current_api_calls_today,
    ROUND(q.current_api_calls_today::DECIMAL / q.max_api_calls_per_day * 100, 2) AS api_usage_percent
FROM tenant t
INNER JOIN tenant_resource_quota q ON t.id = q.tenant_id
WHERE NOT t.deleted;

COMMENT ON VIEW v_tenant_quota_usage IS '租户配额使用情况视图';

-- ======================================================================
-- 视图：即将过期的租户
-- ======================================================================
CREATE OR REPLACE VIEW v_expiring_tenants AS
SELECT
    t.id,
    t.tenant_code,
    t.tenant_name,
    t.contact_name,
    t.contact_email,
    s.end_date,
    s.end_date - CURRENT_DATE AS days_to_expire,
    s.auto_renew,
    p.package_name
FROM tenant t
INNER JOIN tenant_subscription s ON t.id = s.tenant_id
INNER JOIN tenant_package p ON s.package_id = p.id
WHERE s.status = 1
  AND s.end_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '30 days'
  AND NOT t.deleted
ORDER BY s.end_date ASC;

COMMENT ON VIEW v_expiring_tenants IS '即将过期的租户视图（未来30天）';

-- ======================================================================
-- 函数：检查租户配额是否充足
-- ======================================================================
CREATE OR REPLACE FUNCTION fn_check_tenant_quota(
    p_tenant_id UUID,
    p_quota_type VARCHAR,
    p_increment INT DEFAULT 1
)
RETURNS JSONB AS $$
DECLARE
    v_quota RECORD;
    v_max_value INT;
    v_current_value INT;
BEGIN
    -- 获取配额记录
    SELECT * INTO v_quota
    FROM tenant_resource_quota
    WHERE tenant_id = p_tenant_id;

    IF NOT FOUND THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'QUOTA_NOT_FOUND',
            'message', '租户配额不存在'
        );
    END IF;

    -- 根据配额类型检查
    CASE p_quota_type
        WHEN 'USERS' THEN
            v_max_value := v_quota.max_users;
            v_current_value := v_quota.current_users;
        WHEN 'WAREHOUSES' THEN
            v_max_value := v_quota.max_warehouses;
            v_current_value := v_quota.current_warehouses;
        WHEN 'SKUS' THEN
            v_max_value := v_quota.max_skus;
            v_current_value := v_quota.current_skus;
        WHEN 'ORDERS' THEN
            v_max_value := v_quota.max_orders_per_day;
            v_current_value := v_quota.current_orders_today;
        WHEN 'API' THEN
            v_max_value := v_quota.max_api_calls_per_day;
            v_current_value := v_quota.current_api_calls_today;
        ELSE
            RETURN jsonb_build_object(
                'success', false,
                'error', 'INVALID_QUOTA_TYPE',
                'message', '无效的配额类型'
            );
    END CASE;

    -- 检查是否超限
    IF v_current_value + p_increment > v_max_value THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'QUOTA_EXCEEDED',
            'message', '配额已用尽',
            'max_value', v_max_value,
            'current_value', v_current_value,
            'requested', p_increment
        );
    END IF;

    RETURN jsonb_build_object(
        'success', true,
        'max_value', v_max_value,
        'current_value', v_current_value,
        'available', v_max_value - v_current_value
    );
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION fn_check_tenant_quota IS '检查租户配额是否充足';

-- ======================================================================
-- 注意：
-- 1. 所有业务表必须添加 tenant_id 字段
-- 2. 每日配额（订单、API）需要定时任务重置
-- 3. 租户数据隔离通过应用层 + tenant_id 过滤实现
-- 4. 敏感配置（如API密钥）应加密存储
-- 5. 操作日志按月分区，需定期添加新分区
-- ======================================================================