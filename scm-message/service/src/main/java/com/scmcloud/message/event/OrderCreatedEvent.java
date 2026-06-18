package com.scmcloud.message.event;

import lombok.Builder;
import lombok.Data;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class OrderCreatedEvent implements DomainEvent {
    private String eventId;
    private String orderId;
    private String orderNo;
    private Long tenantId;
    private Date timestamp;
    
    @Override
    public String getEventType() {
        return "ORDER_CREATED";
    }
    
    @Override
    public String getAggregateType() {
        return "Order";
    }
    
    @Override
    public String getAggregateId() {
        return orderId;
    }
    
    @Override
    public Map<String, Object> getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("orderNo", orderNo);
        return payload;
    }
    
    public static OrderCreatedEvent of(String orderId, String orderNo, Long tenantId) {
        return OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .orderNo(orderNo)
                .tenantId(tenantId)
                .timestamp(new Date())
                .build();
    }
}
