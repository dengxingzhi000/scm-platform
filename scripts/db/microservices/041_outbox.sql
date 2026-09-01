-- ======================================================================
-- Transactional Outbox — RUN IN: db_order AND db_inventory (and db_analytics if central relay)
-- 职责：业务库与事务同写、异步投递至 Kafka 的 CDC 事件表
-- 设计：业务聚合变更与 outbox insert 在同一 DB 事务内完成，
--      由 OutboxRelayJob / OutboxPoller 异步轮询 published=false 的记录
--      投递至 Kafka topic "scm.<aggregate_type>"，成功后标记已发布
-- 执行说明：本文件为单文件模板，必须在 db_order 与 db_inventory 分别执行一次；
--           若 analytics 集中轮询则在 db_analytics 也执行一份。
-- ======================================================================

-- 通用 outbox_event（必须在 db_order 与 db_inventory 各库分别执行此脚本）
-- 供 analytics 的 RelayJob 轮询；若集中投递可仅在 db_order 落表，inventory 作为模板复用

CREATE TABLE IF NOT EXISTS outbox_event (
    id              VARCHAR(36) PRIMARY KEY,
    tenant_id       UUID            NOT NULL,
    aggregate_type  VARCHAR(64)     NOT NULL,
    aggregate_id    VARCHAR(64)     NOT NULL,
    event_type      VARCHAR(64)     NOT NULL,
    payload         JSONB           NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    published       BOOLEAN         NOT NULL DEFAULT false,
    published_at    TIMESTAMPTZ
);

-- 高效轮询未投递事件（partial index）
CREATE INDEX IF NOT EXISTS idx_outbox_unpublished
    ON outbox_event (published, created_at)
    WHERE published = false;

-- 按聚合查询（幂等/排查）— 可选
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate
    ON outbox_event (aggregate_type, aggregate_id);

CREATE INDEX IF NOT EXISTS idx_outbox_tenant
    ON outbox_event (tenant_id);

COMMENT ON TABLE outbox_event IS 'Transactional outbox for CDC: written atomically with aggregate, relayed to Kafka';
COMMENT ON COLUMN outbox_event.id IS '主键 UUID 字符串';
COMMENT ON COLUMN outbox_event.tenant_id IS '租户ID，来自 TenantContextHolder.getRequiredTenantId()';
COMMENT ON COLUMN outbox_event.aggregate_type IS '聚合根类型，如 OrdOrder / Inventory';
COMMENT ON COLUMN outbox_event.aggregate_id IS '聚合ID（订单号/库存ID等）';
COMMENT ON COLUMN outbox_event.event_type IS '事件类型，如 order.created / inventory.adjusted';
COMMENT ON COLUMN outbox_event.payload IS '事件载荷 JSONB';
COMMENT ON COLUMN outbox_event.published IS '是否已投递至 Kafka';
COMMENT ON COLUMN outbox_event.published_at IS '投递时间';
