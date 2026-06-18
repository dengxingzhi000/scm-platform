package com.scmcloud.message.producer;

import com.scmcloud.message.entity.EventOutbox;
import com.scmcloud.message.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxService outboxService;
    private final KafkaEventProducer kafkaEventProducer;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<EventOutbox> pendingEvents = outboxService.findPendingEvents(100);

        for (EventOutbox event : pendingEvents) {
            publishEvent(event);
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void publishRetryingEvents() {
        List<EventOutbox> retryingEvents = outboxService.findRetryingEvents(50);

        for (EventOutbox event : retryingEvents) {
            publishEvent(event);
        }
    }

    private void publishEvent(EventOutbox event) {
        try {
            DomainEvent domainEvent = convertToDomainEvent(event);

            kafkaEventProducer.send(domainEvent)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            outboxService.markAsPublished(event.getId());
                        } else {
                            outboxService.markAsFailed(event.getId(), ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event.getId(), e);
            outboxService.markAsFailed(event.getId(), e.getMessage());
        }
    }

    private DomainEvent convertToDomainEvent(EventOutbox outbox) {
        return new DomainEvent() {
            @Override
            public String getEventId() { return outbox.getId(); }
            @Override
            public String getEventType() { return outbox.getEventType(); }
            @Override
            public String getAggregateType() { return outbox.getAggregateType(); }
            @Override
            public String getAggregateId() { return outbox.getAggregateId(); }
            @Override
            public Long getTenantId() { return outbox.getTenantId(); }
            @Override
            public Date getTimestamp() { return outbox.getCreateTime(); }
            @Override
            public Map<String, Object> getPayload() { return Collections.emptyMap(); }
        };
    }
}
