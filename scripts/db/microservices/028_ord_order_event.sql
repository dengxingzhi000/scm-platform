-- ======================================================================
-- 订单本地事件溯源存储 (db_order)
-- append-only;与 ord_status_history 并存;跨服务消息由 outbox 负责
-- 保留策略:retention/apply-retention.sh 已含 3 年清理
--
-- 注意:order_id 改为 UUID,与 ord_order.id 类型对齐。
--      id 保留 BIGSERIAL 作为内部自增序列(仅本地,不参与跨服务标识)。
-- ======================================================================

CREATE TABLE IF NOT EXISTS ord_order_event (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   UUID         NOT NULL,
    event_id    UUID         NOT NULL,
    order_id    UUID         NOT NULL,
    order_no    VARCHAR(128),
    event_type  VARCHAR(64)  NOT NULL,
    event_data  TEXT         NOT NULL,
    create_time TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_ord_order_event_event_id UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_ord_order_event_order
    ON ord_order_event (order_id, create_time);

COMMENT ON TABLE ord_order_event IS '订单本地事件溯源存储(append-only,本地审计/重放)';
