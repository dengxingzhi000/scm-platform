package com.scmcloud.order.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 订单创建事件(本地事件溯源用)。
 */
@Getter
public class OrderCreatedEvent extends OrderEvent {
    private final String userId;
    private final BigDecimal totalAmount;
    private final BigDecimal payableAmount;

    public OrderCreatedEvent(UUID tenantId, UUID orderId, String orderNo,
                             String userId, BigDecimal totalAmount, BigDecimal payableAmount) {
        super(tenantId, orderId, orderNo, "ORDER_CREATED");
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.payableAmount = payableAmount;
    }

    @JsonCreator
    public OrderCreatedEvent(@JsonProperty("eventId") UUID eventId,
                              @JsonProperty("tenantId") UUID tenantId,
                              @JsonProperty("orderId") UUID orderId,
                              @JsonProperty("orderNo") String orderNo,
                              @JsonProperty("timestamp") Instant timestamp,
                              @JsonProperty("eventType") String eventType,
                              @JsonProperty("userId") String userId,
                              @JsonProperty("totalAmount") BigDecimal totalAmount,
                              @JsonProperty("payableAmount") BigDecimal payableAmount) {
        super(eventId, tenantId, orderId, orderNo, timestamp, eventType);
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.payableAmount = payableAmount;
    }
}
