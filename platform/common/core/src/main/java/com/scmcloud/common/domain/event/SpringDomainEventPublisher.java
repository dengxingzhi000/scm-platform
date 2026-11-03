package com.scmcloud.common.domain.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Default implementation using Spring's ApplicationEventPublisher.
 * Events are delivered synchronously to in-process listeners.
 *
 * <p>For cross-service events, use the transactional outbox pattern (P2-4)
 * which will write events to a DB table and poll to Kafka.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(DomainEvent event) {
        log.debug("Publishing domain event: {}", event);
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishAsync(DomainEvent event) {
        log.debug("Publishing domain event async: {}", event);
        applicationEventPublisher.publishEvent(event);
        // Note: true async requires @Async on listeners or an ApplicationEventMulticaster
        // with a TaskExecutor. For now, delegates to sync publish.
    }
}
