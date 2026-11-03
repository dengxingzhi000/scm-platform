package com.scmcloud.common.domain.event;

import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Base class for all domain events.
 * Events are immutable and carry a unique ID, timestamp, and optional tenant/correlation context.
 *
 * <p>Usage: extend this class for each domain event type.
 * Publish via {@link DomainEventPublisher} or Spring's ApplicationEventPublisher.</p>
 */
@Getter
public abstract class DomainEvent {

    private final String eventId;
    private final OffsetDateTime occurredAt;
    private final UUID tenantId;
    private final String correlationId;

    protected DomainEvent() {
        this(null, null);
    }

    protected DomainEvent(UUID tenantId) {
        this(tenantId, null);
    }

    protected DomainEvent(UUID tenantId, String correlationId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.tenantId = tenantId;
        this.correlationId = correlationId;
    }

    /**
     * Event type identifier for serialization and routing.
     * Default: simple class name. Override for custom types.
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return getEventType() + "{" +
                "eventId='" + eventId + '\'' +
                ", occurredAt=" + occurredAt +
                ", tenantId=" + tenantId +
                '}';
    }
}
