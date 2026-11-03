package com.scmcloud.common.domain.event;

/**
 * Publishes domain events. Implementations may use Spring ApplicationEventPublisher,
 * Kafka, or the transactional outbox pattern.
 *
 * <p>Inject this interface in domain services to publish events.
 * The default Spring implementation delegates to ApplicationEventPublisher.</p>
 */
public interface DomainEventPublisher {

    /**
     * Publish a domain event synchronously (in-process listeners execute immediately).
     */
    void publish(DomainEvent event);

    /**
     * Publish a domain event asynchronously (listeners execute in a separate thread).
     */
    default void publishAsync(DomainEvent event) {
        publish(event);
    }
}
