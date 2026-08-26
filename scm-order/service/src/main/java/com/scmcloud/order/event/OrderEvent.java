package com.scmcloud.order.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderCreatedEvent.class, name = "ORDER_CREATED"),
    @JsonSubTypes.Type(value = OrderStatusChangedEvent.class, name = "ORDER_STATUS_CHANGED")
})
public abstract class OrderEvent {

    private final UUID eventId;
    private final UUID tenantId;
    private final Long orderId;
    private final String orderNo;
    private final Instant timestamp;
    private final String eventType;

    protected OrderEvent(UUID tenantId, Long orderId, String orderNo, String eventType) {
        this(UUID.randomUUID(), tenantId, orderId, orderNo, Instant.now(), eventType);
    }

    @JsonCreator
    protected OrderEvent(@JsonProperty("eventId") UUID eventId,
                          @JsonProperty("tenantId") UUID tenantId,
                          @JsonProperty("orderId") Long orderId,
                          @JsonProperty("orderNo") String orderNo,
                          @JsonProperty("timestamp") Instant timestamp,
                          @JsonProperty("eventType") String eventType) {
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.timestamp = timestamp;
        this.eventType = eventType;
    }

    public UUID getEventId() { return eventId; }
    public UUID getTenantId() { return tenantId; }
    public Long getOrderId() { return orderId; }
    public String getOrderNo() { return orderNo; }
    public Instant getTimestamp() { return timestamp; }
    public String getEventType() { return eventType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderEvent other)) return false;
        return eventId.equals(other.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{eventId=" + eventId + ", orderId=" + orderId
                + ", orderNo='" + orderNo + "', eventType='" + eventType + "'}";
    }
}
