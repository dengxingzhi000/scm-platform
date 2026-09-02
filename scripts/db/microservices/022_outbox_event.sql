-- ============================================================
-- Transactional Outbox Table
-- Events written in the same DB transaction as the entity,
-- polled by a background process and published to Kafka.
-- ============================================================

CREATE TABLE IF NOT EXISTS outbox_event (
    id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    aggregate_type VARCHAR(128) NOT NULL,
    aggregate_id VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    tenant_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 5,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    last_error TEXT,
    next_retry_at TIMESTAMPTZ
);

-- Index for polling unpublished events
CREATE INDEX IF NOT EXISTS idx_outbox_status_created
    ON outbox_event(status, created_at)
    WHERE status = 'PENDING';

-- Index for retry scheduling
CREATE INDEX IF NOT EXISTS idx_outbox_retry
    ON outbox_event(next_retry_at)
    WHERE status = 'FAILED' AND retry_count < max_retries;

-- Index for cleanup
CREATE INDEX IF NOT EXISTS idx_outbox_published
    ON outbox_event(published_at)
    WHERE status = 'PUBLISHED';

-- Index for tenant-scoped queries (CQRS / audit)
CREATE INDEX IF NOT EXISTS idx_outbox_tenant
    ON outbox_event(tenant_id);

-- Comments
COMMENT ON TABLE outbox_event IS 'Transactional outbox for reliable event publishing to Kafka';
COMMENT ON COLUMN outbox_event.id IS 'Event ID (UUID)';
COMMENT ON COLUMN outbox_event.event_type IS 'Event type identifier (e.g., order.created)';
COMMENT ON COLUMN outbox_event.aggregate_type IS 'Aggregate type (e.g., OrdOrder)';
COMMENT ON COLUMN outbox_event.aggregate_id IS 'Aggregate ID';
COMMENT ON COLUMN outbox_event.payload IS 'Event payload as JSON';
COMMENT ON COLUMN outbox_event.status IS 'PENDING, PUBLISHED, FAILED';
COMMENT ON COLUMN outbox_event.retry_count IS 'Number of publish attempts';
