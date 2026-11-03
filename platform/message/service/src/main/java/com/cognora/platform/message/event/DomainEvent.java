package com.cognora.platform.message.event;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

public interface DomainEvent {
    String getEventId();
    String getEventType();
    String getAggregateType();
    String getAggregateId();
    String getTenantId();
    Date getTimestamp();
    Map<String, Object> getPayload();
    
    default String generateEventId() {
        return UUID.randomUUID().toString();
    }
}
