-- ======================================================================
-- Analytics governance schema (db_analytics)
-- Flyway migration V2 — lineage edges, quality rules, quality results
-- ======================================================================

-- ======================================================================
-- 1. 数据血缘边 (analytics_lineage_edge)
-- ======================================================================
CREATE TABLE IF NOT EXISTS analytics_lineage_edge (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    upstream_node VARCHAR(160) NOT NULL,
    downstream_node VARCHAR(160) NOT NULL,
    edge_type VARCHAR(32) NOT NULL DEFAULT 'TABLE'
        CONSTRAINT chk_lineage_edge_type CHECK (edge_type IN ('TABLE', 'COLUMN', 'METRIC', 'JOB')),
    job_name VARCHAR(128),

    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uk_lineage_edge UNIQUE (tenant_id, upstream_node, downstream_node, edge_type)
);

CREATE INDEX idx_lineage_tenant ON analytics_lineage_edge(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_lineage_upstream ON analytics_lineage_edge(tenant_id, upstream_node) WHERE NOT deleted;
CREATE INDEX idx_lineage_downstream ON analytics_lineage_edge(tenant_id, downstream_node) WHERE NOT deleted;

COMMENT ON TABLE analytics_lineage_edge IS '数据血缘边：ODS -> DWD -> DWS -> ADS -> 指标';
COMMENT ON COLUMN analytics_lineage_edge.upstream_node IS '上游节点，如 analytics.ods_order';
COMMENT ON COLUMN analytics_lineage_edge.downstream_node IS '下游节点，如 analytics.dwd_sales_detail';

-- ======================================================================
-- 2. 数据质量规则 (analytics_quality_rule)
-- ======================================================================
CREATE TABLE IF NOT EXISTS analytics_quality_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    description TEXT,
    dataset_code VARCHAR(64) NOT NULL,
    rule_type VARCHAR(32) NOT NULL
        CONSTRAINT chk_quality_rule_type CHECK (rule_type IN ('NOT_NULL', 'RANGE', 'UNIQUE', 'FRESHNESS')),
    target_field VARCHAR(64) NOT NULL,
    config JSONB NOT NULL DEFAULT '{}'::jsonb,
    severity VARCHAR(16) NOT NULL DEFAULT 'MEDIUM'
        CONSTRAINT chk_quality_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    status SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_quality_status CHECK (status IN (0, 1)),

    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT uk_quality_rule_tenant_code UNIQUE (tenant_id, rule_code)
);

CREATE INDEX idx_quality_rule_tenant ON analytics_quality_rule(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_quality_rule_dataset ON analytics_quality_rule(tenant_id, dataset_code) WHERE NOT deleted;
CREATE INDEX idx_quality_rule_enabled ON analytics_quality_rule(enabled) WHERE NOT deleted;

COMMENT ON TABLE analytics_quality_rule IS '数据质量规则定义';
COMMENT ON COLUMN analytics_quality_rule.config IS '规则参数，如 {"min":0,"max":100} 或 {"maxAgeHours":24}';

-- ======================================================================
-- 3. 数据质量检查结果 (analytics_quality_result)
-- ======================================================================
CREATE TABLE IF NOT EXISTS analytics_quality_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    rule_id UUID NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    dataset_code VARCHAR(64) NOT NULL,
    check_date DATE NOT NULL,
    passed BOOLEAN NOT NULL,
    metric_value DECIMAL(18, 4),
    message TEXT,

    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_quality_result_rule FOREIGN KEY (rule_id)
        REFERENCES analytics_quality_rule(id) ON DELETE CASCADE
);

CREATE INDEX idx_quality_result_tenant_date
    ON analytics_quality_result(tenant_id, check_date) WHERE NOT deleted;
CREATE INDEX idx_quality_result_rule
    ON analytics_quality_result(rule_id, check_date DESC) WHERE NOT deleted;

COMMENT ON TABLE analytics_quality_result IS '数据质量检查结果';
COMMENT ON COLUMN analytics_quality_result.passed IS '是否通过；UNIQUE/NOT_NULL/RANGE 为违规行数为 0';
