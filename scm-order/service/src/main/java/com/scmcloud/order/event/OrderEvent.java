package com.scmcloud.order.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderCreatedEvent.class, name = "ORDER_CREATED"),
    @JsonSubTypes.Type(value = OrderStatusChangedEvent.class, name = "ORDER_STATUS_CHANGED")
})
@Getter
public abstract class OrderEvent {
    private final UUID eventId;
    private final UUID tenantId;
    private final UUID orderId;
    private final String orderNo;
    private final Instant timestamp;
    private final String eventType;

    protected OrderEvent(UUID tenantId, UUID orderId, String orderNo, String eventType) {
        this(UUID.randomUUID(), tenantId, orderId, orderNo, Instant.now(), eventType);
    }

    @JsonCreator
    protected OrderEvent(@JsonProperty("eventId") UUID eventId,
                          @JsonProperty("tenantId") UUID tenantId,
                          @JsonProperty("orderId") UUID orderId,
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
}
