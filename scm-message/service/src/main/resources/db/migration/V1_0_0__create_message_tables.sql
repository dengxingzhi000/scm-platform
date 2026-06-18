-- Event outbox table for transactional event publishing
CREATE TABLE sys_event_outbox (
    id              VARCHAR(36) PRIMARY KEY,
    event_type      VARCHAR(100) NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(36) NOT NULL,
    payload         TEXT NOT NULL,
    retry_count     INTEGER DEFAULT 0,
    max_retries     INTEGER DEFAULT 3,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message   TEXT,
    tenant_id       BIGINT NOT NULL,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at    TIMESTAMP,
    next_retry_at   TIMESTAMP
);

CREATE INDEX idx_outbox_status ON sys_event_outbox(status);
CREATE INDEX idx_outbox_next_retry ON sys_event_outbox(next_retry_at);
CREATE INDEX idx_outbox_aggregate ON sys_event_outbox(aggregate_type, aggregate_id);

-- Dead letter queue table
CREATE TABLE sys_event_dlq (
    id              VARCHAR(36) PRIMARY KEY,
    original_event_id VARCHAR(36) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(36) NOT NULL,
    payload         TEXT NOT NULL,
    error_message   TEXT,
    error_stack     TEXT,
    retry_count     INTEGER DEFAULT 0,
    tenant_id       BIGINT NOT NULL,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved        BOOLEAN DEFAULT FALSE,
    resolved_at     TIMESTAMP
);

CREATE INDEX idx_dlq_event_type ON sys_event_dlq(event_type);
CREATE INDEX idx_dlq_resolved ON sys_event_dlq(resolved);
