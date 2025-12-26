-- ======================================================================
-- 商品服务数据库 (db_product)
-- 职责：商品SPU/SKU管理、分类、品牌、属性模板
-- ======================================================================

-- 创建数据库
-- CREATE DATABASE db_product WITH ENCODING = 'UTF8';

-- 启用必要扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- 全文搜索

-- ======================================================================
-- 1. 商品分类表 (prod_category)
-- ======================================================================
CREATE TABLE IF NOT EXISTS prod_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_code VARCHAR(64) NOT NULL UNIQUE,
    category_name VARCHAR(128) NOT NULL,
    parent_id UUID,
    level SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_category_level CHECK (level BETWEEN 1 AND 5),
    sort_order INT NOT NULL DEFAULT 0,
    icon_url VARCHAR(512),
    image_url VARCHAR(512),
    description TEXT,
    is_leaf BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- SEO
    seo_title VARCHAR(256),
    seo_keywords VARCHAR(512),
    seo_description TEXT,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX idx_category_parent ON prod_category(parent_id) WHERE NOT deleted;
CREATE INDEX idx_category_code ON prod_category(category_code) WHERE NOT deleted;
CREATE INDEX idx_category_enabled ON prod_category(enabled, is_leaf) WHERE NOT deleted;
CREATE INDEX idx_category_sort ON prod_category(sort_order);

COMMENT ON TABLE prod_category IS '商品分类表';
COMMENT ON COLUMN prod_category.is_leaf IS '是否叶子分类（只有叶子分类可以挂商品）';

-- ======================================================================
-- 2. 品牌表 (prod_brand)
-- ======================================================================
CREATE TABLE IF NOT EXISTS prod_brand (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    brand_code VARCHAR(64) NOT NULL UNIQUE,
    brand_name VARCHAR(128) NOT NULL,
    brand_name_en VARCHAR(128),
    logo_url VARCHAR(512),
    description TEXT,
    website VARCHAR(256),
    country VARCHAR(64),
    established_year SMALLINT
        CONSTRAINT chk_brand_year CHECK (established_year IS NULL OR (established_year >= 1800 AND established_year <= 2100)),

    -- 状态
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX idx_brand_code ON prod_brand(brand_code) WHERE NOT deleted;
CREATE INDEX idx_brand_enabled ON prod_brand(enabled) WHERE NOT deleted;
CREATE INDEX idx_brand_featured ON prod_brand(featured) WHERE enabled = TRUE AND NOT deleted;
CREATE INDEX idx_brand_name_trgm ON prod_brand USING gin (brand_name gin_trgm_ops) WHERE NOT deleted;

COMMENT ON TABLE prod_brand IS '商品品牌表';
COMMENT ON COLUMN prod_brand.featured IS '是否推荐品牌';

-- ======================================================================
-- 3. SPU表 (prod_spu) - 标准产品单元
-- ======================================================================
CREATE TABLE IF NOT EXISTS prod_spu (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    spu_code VARCHAR(128) NOT NULL UNIQUE,
    spu_name VARCHAR(256) NOT NULL,
    category_id UUID NOT NULL,
    brand_id UUID,

    -- 内容
    description TEXT,
    detail_html TEXT,
    images JSONB DEFAULT '[]',
    main_image VARCHAR(512),
    video_url VARCHAR(512),

    -- 价格区间（从SKU计算）
    min_price DECIMAL(12, 2),
    max_price DECIMAL(12, 2),

    -- 库存统计（从SKU计算）
    total_stock INT DEFAULT 0,
    total_sales INT DEFAULT 0,

    -- 显示
    sort_order INT NOT NULL DEFAULT 0,

    -- SEO
    seo_title VARCHAR(256),
    seo_keywords VARCHAR(512),
    seo_description TEXT,

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_spu_status CHECK (status IN (0, 1, 2, 3)),
    published_at TIMESTAMPTZ,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT fk_spu_category FOREIGN KEY (category_id) REFERENCES prod_category(id),
    CONSTRAINT fk_spu_brand FOREIGN KEY (brand_id) REFERENCES prod_brand(id)
);

CREATE INDEX idx_spu_code ON prod_spu(spu_code) WHERE NOT deleted;
CREATE INDEX idx_spu_category ON prod_spu(category_id) WHERE NOT deleted;
CREATE INDEX idx_spu_brand ON prod_spu(brand_id) WHERE NOT deleted;
CREATE INDEX idx_spu_status ON prod_spu(status) WHERE NOT deleted;
CREATE INDEX idx_spu_price ON prod_spu(min_price, max_price) WHERE status = 1 AND NOT deleted;
CREATE INDEX idx_spu_sales ON prod_spu(total_sales DESC) WHERE status = 1 AND NOT deleted;
CREATE INDEX idx_spu_name_trgm ON prod_spu USING gin (spu_name gin_trgm_ops) WHERE NOT deleted;

COMMENT ON TABLE prod_spu IS 'SPU标准产品单元表';
COMMENT ON COLUMN prod_spu.status IS '状态:0-草稿,1-上架,2-下架,3-删除';
COMMENT ON COLUMN prod_spu.images IS 'JSON数组，存储图片URL列表';

-- ======================================================================
-- 4. SKU表 (prod_sku) - 库存单位
-- ======================================================================
CREATE TABLE IF NOT EXISTS prod_sku (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    spu_id UUID NOT NULL,
    sku_code VARCHAR(128) NOT NULL UNIQUE,
    sku_name VARCHAR(256) NOT NULL,

    -- 属性（JSON格式，如：{"color":"黑色","storage":"256GB"}）
    attributes JSONB NOT NULL DEFAULT '{}',

    -- 价格
    original_price DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_sku_original_price CHECK (original_price >= 0),
    selling_price DECIMAL(12, 2) NOT NULL
        CONSTRAINT chk_sku_selling_price CHECK (selling_price >= 0),
    cost_price DECIMAL(12, 2)
        CONSTRAINT chk_sku_cost_price CHECK (cost_price IS NULL OR cost_price >= 0),

    -- 库存（从库存服务同步）
    stock INT DEFAULT 0
        CONSTRAINT chk_sku_stock CHECK (stock >= 0),
    available_stock INT DEFAULT 0
        CONSTRAINT chk_sku_available_stock CHECK (available_stock >= 0),
    locked_stock INT DEFAULT 0
        CONSTRAINT chk_sku_locked_stock CHECK (locked_stock >= 0),

    -- 销量
    sales_count INT DEFAULT 0
        CONSTRAINT chk_sku_sales_count CHECK (sales_count >= 0),

    -- 物理属性
    weight DECIMAL(10, 2)
        CONSTRAINT chk_sku_weight CHECK (weight IS NULL OR weight >= 0),
    volume DECIMAL(10, 2)
        CONSTRAINT chk_sku_volume CHECK (volume IS NULL OR volume >= 0),
    barcode VARCHAR(64),

    -- 图片
    image_url VARCHAR(512),
    images JSONB DEFAULT '[]',

    -- 状态
    status SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_sku_status CHECK (status IN (0, 1, 2, 3)),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT fk_sku_spu FOREIGN KEY (spu_id) REFERENCES prod_spu(id)
);

CREATE INDEX idx_sku_code ON prod_sku(sku_code) WHERE NOT deleted;
CREATE INDEX idx_sku_spu ON prod_sku(spu_id) WHERE NOT deleted;
CREATE INDEX idx_sku_status ON prod_sku(status) WHERE NOT deleted;
CREATE INDEX idx_sku_barcode ON prod_sku(barcode) WHERE NOT deleted AND barcode IS NOT NULL;
CREATE INDEX idx_sku_stock ON prod_sku(available_stock) WHERE status = 1 AND NOT deleted;

COMMENT ON TABLE prod_sku IS 'SKU库存单位表';
COMMENT ON COLUMN prod_sku.status IS '状态:0-停用,1-启用,2-缺货,3-删除';
COMMENT ON COLUMN prod_sku.attributes IS 'SKU属性JSON，如：{"color":"黑色","storage":"256GB"}';

-- ======================================================================
-- 5. 属性模板表 (prod_attribute_template)
-- ======================================================================
CREATE TABLE IF NOT EXISTS prod_attribute_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_name VARCHAR(128) NOT NULL,
    category_id UUID,

    -- 属性定义（JSON数组）
    -- 示例：[{"name":"color","label":"颜色","type":"select","required":true,"options":["红色","蓝色"]}]
    attributes JSONB NOT NULL DEFAULT '[]',

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT fk_template_category FOREIGN KEY (category_id) REFERENCES prod_category(id)
);

CREATE INDEX idx_template_category ON prod_attribute_template(category_id) WHERE NOT deleted;

COMMENT ON TABLE prod_attribute_template IS '商品属性模板表';
COMMENT ON COLUMN prod_attribute_template.attributes IS '属性定义JSON数组';

-- ======================================================================
-- 注意：
-- 1. update_time 由 MyBatis-Plus MetaObjectHandler 自动填充
-- 2. UUIDv7 提供时间有序性，便于范围查询和索引性能
-- 3. 分类采用简化的层级结构，不使用复杂的ancestors路径
-- ======================================================================