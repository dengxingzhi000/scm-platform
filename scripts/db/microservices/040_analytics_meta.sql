-- ======================================================================
-- Analytics metadata schema (db_analytics) — DBA init script
-- 职责：指标、维度、数据集及字段的元数据管理
-- Identical copy of scm-analytics/service/src/main/resources/db/migration/V1__init_analytics_meta.sql
-- Apply via: psql -d db_analytics -f 040_analytics_meta.sql
-- Flyway migration V1 — creates analytics_metric / analytics_dimension /
-- analytics_dataset / analytics_dataset_field
-- ======================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ======================================================================
-- 1. 指标表 (analytics_metric)
-- ======================================================================
CREATE TABLE IF NOT EXISTS analytics_metric (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    metric_code VARCHAR(64) NOT NULL,
    metric_name VARCHAR(128) NOT NULL,
    description TEXT,
    metric_type VARCHAR(32) NOT NULL DEFAULT 'BASE'
        CONSTRAINT chk_metric_type CHECK (metric_type IN ('BASE', 'DERIVED', 'COMPOSITE')),
    data_type VARCHAR(32) NOT NULL DEFAULT 'DECIMAL'
        CONSTRAINT chk_metric_data_type CHECK (data_type IN ('DECIMAL', 'BIGINT', 'DOUBLE', 'INT', 'STRING')),
    agg_func VARCHAR(32)
        CONSTRAINT chk_metric_agg CHECK (agg_func IS NULL OR agg_func IN ('SUM', 'AVG', 'COUNT', 'MAX', 'MIN', 'COUNT_DISTINCT', 'NONE')),
    expr TEXT,
    unit VARCHAR(32),
    format VARCHAR(64),
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    status SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_metric_status CHECK (status IN (0, 1)),
    sort_order INT NOT NULL DEFAULT 0,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT uk_metric_tenant_code UNIQUE (tenant_id, metric_code)
);

CREATE INDEX idx_metric_tenant ON analytics_metric(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_metric_tenant_code ON analytics_metric(tenant_id, metric_code) WHERE NOT deleted;
CREATE INDEX idx_metric_status ON analytics_metric(status) WHERE NOT deleted;

COMMENT ON TABLE analytics_metric IS '指标定义表';
COMMENT ON COLUMN analytics_metric.tenant_id IS '租户ID，指标按租户隔离，code 在租户内唯一';
COMMENT ON COLUMN analytics_metric.metric_code IS '指标编码，租户内唯一';
COMMENT ON COLUMN analytics_metric.metric_type IS '指标类型:BASE-原子,DERIVED-派生,COMPOSITE-复合';
COMMENT ON COLUMN analytics_metric.agg_func IS '聚合函数:SUM/AVG/COUNT/MAX/MIN/COUNT_DISTINCT/NONE';

-- ======================================================================
-- 2. 维度表 (analytics_dimension)
-- ======================================================================
CREATE TABLE IF NOT EXISTS analytics_dimension (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    dim_code VARCHAR(64) NOT NULL,
    dim_name VARCHAR(128) NOT NULL,
    description TEXT,
    dim_type VARCHAR(32) NOT NULL DEFAULT 'CATEGORICAL'
        CONSTRAINT chk_dim_type CHECK (dim_type IN ('TIME', 'CATEGORICAL', 'NUMERIC', 'GEO', 'HIERARCHICAL')),
    data_type VARCHAR(32) NOT NULL DEFAULT 'VARCHAR'
        CONSTRAINT chk_dim_data_type CHECK (data_type IN ('VARCHAR', 'INT', 'BIGINT', 'DATE', 'TIMESTAMPTZ', 'DECIMAL', 'BOOLEAN')),
    parent_dim_id UUID,
    hierarchy_level SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_dim_level CHECK (hierarchy_level BETWEEN 1 AND 5),
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    dict_code VARCHAR(64),
    status SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_dim_status CHECK (status IN (0, 1)),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT uk_dimension_tenant_code UNIQUE (tenant_id, dim_code),
    CONSTRAINT fk_dimension_parent FOREIGN KEY (parent_dim_id) REFERENCES analytics_dimension(id) ON DELETE SET NULL
);

CREATE INDEX idx_dimension_tenant ON analytics_dimension(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_dimension_tenant_code ON analytics_dimension(tenant_id, dim_code) WHERE NOT deleted;
CREATE INDEX idx_dimension_parent ON analytics_dimension(parent_dim_id) WHERE NOT deleted;
CREATE INDEX idx_dimension_status ON analytics_dimension(status) WHERE NOT deleted;

COMMENT ON TABLE analytics_dimension IS '维度定义表';
COMMENT ON COLUMN analytics_dimension.tenant_id IS '租户ID，维度按租户隔离，code 在租户内唯一';
COMMENT ON COLUMN analytics_dimension.dim_code IS '维度编码，租户内唯一';
COMMENT ON COLUMN analytics_dimension.dim_type IS '维度类型:TIME-时间,CATEGORICAL-分类,NUMERIC-数值,GEO-地理,HIERARCHICAL-层级';

-- ======================================================================
-- 3. 数据集表 (analytics_dataset)
-- ======================================================================
CREATE TABLE IF NOT EXISTS analytics_dataset (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    dataset_code VARCHAR(64) NOT NULL,
    dataset_name VARCHAR(128) NOT NULL,
    description TEXT,
    dataset_type VARCHAR(32) NOT NULL DEFAULT 'WIDE_TABLE'
        CONSTRAINT chk_dataset_type CHECK (dataset_type IN ('WIDE_TABLE', 'VIEW', 'API', 'CUSTOM')),
    source_type VARCHAR(32) NOT NULL DEFAULT 'CLICKHOUSE'
        CONSTRAINT chk_dataset_source CHECK (source_type IN ('CLICKHOUSE', 'POSTGRES', 'MYSQL', 'API')),
    source_table VARCHAR(128),
    source_config JSONB DEFAULT '{}',
    status SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_dataset_status CHECK (status IN (0, 1)),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT uk_dataset_tenant_code UNIQUE (tenant_id, dataset_code)
);

CREATE INDEX idx_dataset_tenant ON analytics_dataset(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_dataset_tenant_code ON analytics_dataset(tenant_id, dataset_code) WHERE NOT deleted;
CREATE INDEX idx_dataset_source ON analytics_dataset(source_type, source_table) WHERE NOT deleted;
CREATE INDEX idx_dataset_status ON analytics_dataset(status) WHERE NOT deleted;

COMMENT ON TABLE analytics_dataset IS '数据集定义表';
COMMENT ON COLUMN analytics_dataset.tenant_id IS '租户ID，数据集按租户隔离，code 在租户内唯一';
COMMENT ON COLUMN analytics_dataset.dataset_code IS '数据集编码，租户内唯一';
COMMENT ON COLUMN analytics_dataset.source_config IS '数据源配置(JSONB)，如 ClickHouse 连接或 SQL';

-- ======================================================================
-- 4. 数据集字段表 (analytics_dataset_field)
-- ======================================================================
CREATE TABLE IF NOT EXISTS analytics_dataset_field (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    dataset_id UUID NOT NULL,
    field_code VARCHAR(64) NOT NULL,
    field_name VARCHAR(128) NOT NULL,
    description TEXT,
    field_type VARCHAR(32) NOT NULL DEFAULT 'DIMENSION'
        CONSTRAINT chk_field_type CHECK (field_type IN ('METRIC', 'DIMENSION', 'CALCULATED')),
    data_type VARCHAR(32) NOT NULL DEFAULT 'VARCHAR'
        CONSTRAINT chk_field_data_type CHECK (data_type IN ('VARCHAR', 'INT', 'BIGINT', 'DECIMAL', 'DOUBLE', 'DATE', 'TIMESTAMPTZ', 'BOOLEAN')),
    metric_id UUID,
    dimension_id UUID,
    expr TEXT,
    is_partition_key BOOLEAN NOT NULL DEFAULT FALSE,
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    default_value TEXT,
    status SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_field_status CHECK (status IN (0, 1)),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT uk_dataset_field_code UNIQUE (dataset_id, field_code),
    CONSTRAINT fk_field_dataset FOREIGN KEY (dataset_id) REFERENCES analytics_dataset(id) ON DELETE CASCADE,
    CONSTRAINT fk_field_metric FOREIGN KEY (metric_id) REFERENCES analytics_metric(id) ON DELETE SET NULL,
    CONSTRAINT fk_field_dimension FOREIGN KEY (dimension_id) REFERENCES analytics_dimension(id) ON DELETE SET NULL
);

CREATE INDEX idx_field_tenant ON analytics_dataset_field(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_field_dataset ON analytics_dataset_field(dataset_id) WHERE NOT deleted;
CREATE INDEX idx_field_tenant_dataset ON analytics_dataset_field(tenant_id, dataset_id) WHERE NOT deleted;
CREATE INDEX idx_field_metric ON analytics_dataset_field(metric_id) WHERE metric_id IS NOT NULL AND NOT deleted;
CREATE INDEX idx_field_dimension ON analytics_dataset_field(dimension_id) WHERE dimension_id IS NOT NULL AND NOT deleted;
CREATE INDEX idx_field_status ON analytics_dataset_field(status) WHERE NOT deleted;

COMMENT ON TABLE analytics_dataset_field IS '数据集字段表';
COMMENT ON COLUMN analytics_dataset_field.tenant_id IS '租户ID，冗余自数据集以便行级隔离与索引';
COMMENT ON COLUMN analytics_dataset_field.dataset_id IS '所属数据集ID';
COMMENT ON COLUMN analytics_dataset_field.field_code IS '字段编码，数据集内唯一';
COMMENT ON COLUMN analytics_dataset_field.field_type IS '字段类型:METRIC-指标,DIMENSION-维度,CALCULATED-计算字段';
COMMENT ON COLUMN analytics_dataset_field.metric_id IS '关联指标ID（field_type=METRIC时）';
COMMENT ON COLUMN analytics_dataset_field.dimension_id IS '关联维度ID（field_type=DIMENSION时）';

-- ======================================================================
-- 注意：
-- 1. 所有表均包含 tenant_id 且 code 在租户内唯一（UK 约束）
-- 2. update_time 由 MyBatis-Plus MetaObjectHandler 自动填充
-- 3. 数据集字段通过 dataset_id + field_code 保证唯一性
-- ======================================================================
