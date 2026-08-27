package com.scmcloud.order.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.scmcloud.order.domain.entity.OrderStatus;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * 订单状态流转事件(本地事件溯源用)。
 */
@Getter
public class OrderStatusChangedEvent extends OrderEvent {
    private final OrderStatus fromStatus;
    private final OrderStatus toStatus;

    public OrderStatusChangedEvent(UUID tenantId, UUID orderId, String orderNo,
                                   OrderStatus fromStatus, OrderStatus toStatus) {
        super(tenantId, orderId, orderNo, "ORDER_STATUS_CHANGED");
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    @JsonCreator
    public OrderStatusChangedEvent(@JsonProperty("eventId") UUID eventId,
                                    @JsonProperty("tenantId") UUID tenantId,
                                    @JsonProperty("orderId") UUID orderId,
                                    @JsonProperty("orderNo") String orderNo,
                                    @JsonProperty("timestamp") Instant timestamp,
                                    @JsonProperty("eventType") String eventType,
                                    @JsonProperty("fromStatus") OrderStatus fromStatus,
                                    @JsonProperty("toStatus") OrderStatus toStatus) {
        super(eventId, tenantId, orderId, orderNo, timestamp, eventType);
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }
}
