package com.scmcloud.message.producer;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.message.entity.EventOutbox;
import com.scmcloud.message.event.DomainEvent;
import com.scmcloud.message.mapper.EventOutboxMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class OutboxService extends ServiceImpl<EventOutboxMapper, EventOutbox> {
    
    private final ObjectMapper objectMapper;
    
    public OutboxService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    public EventOutbox save(DomainEvent event) {
        try {
            EventOutbox outbox = new EventOutbox();
            outbox.setId(event.getEventId());
            outbox.setEventType(event.getEventType());
            outbox.setAggregateType(event.getAggregateType());
            outbox.setAggregateId(event.getAggregateId());
            outbox.setPayload(objectMapper.writeValueAsString(event.getPayload()));
            outbox.setRetryCount(0);
            outbox.setMaxRetries(3);
            outbox.setStatus("PENDING");
            outbox.setTenantId(event.getTenantId());
            outbox.setCreateTime(new Date());
            outbox.setNextRetryAt(new Date());
            
            save(outbox);
            return outbox;
        } catch (Exception e) {
            log.error("Failed to save event to outbox", e);
            throw new RuntimeException("Failed to save event", e);
        }
    }
    
    public void markAsPublished(String eventId) {
        EventOutbox outbox = getById(eventId);
        if (outbox != null) {
            outbox.setStatus("PUBLISHED");
            outbox.setPublishedAt(new Date());
            updateById(outbox);
        }
    }
    
    public void markAsFailed(String eventId, String errorMessage) {
        EventOutbox outbox = getById(eventId);
        if (outbox != null) {
            outbox.setRetryCount(outbox.getRetryCount() + 1);
            outbox.setErrorMessage(errorMessage);
            
            if (outbox.getRetryCount() >= outbox.getMaxRetries()) {
                outbox.setStatus("FAILED");
            } else {
                outbox.setStatus("RETRYING");
                // Exponential backoff: 1min, 5min, 15min
                long[] backoff = {60000, 300000, 900000};
                long delay = backoff[Math.min(outbox.getRetryCount(), backoff.length - 1)];
                outbox.setNextRetryAt(new Date(System.currentTimeMillis() + delay));
            }
            
            updateById(outbox);
        }
    }
    
    public List<EventOutbox> findPendingEvents(int limit) {
        return baseMapper.findPendingEvents(new Date(), limit);
    }
    
    public List<EventOutbox> findRetryingEvents(int limit) {
        return baseMapper.findRetryingEvents(new Date(), limit);
    }
}
