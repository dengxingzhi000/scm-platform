package com.cognora.platform.message.api;

import com.cognora.platform.message.api.dto.DomainEventDTO;

public interface EventPublisherApi {

    /**
     * Publish event to outbox (transactional)
     */
    void publish(DomainEventDTO event);

    /**
     * Publish event directly to Kafka (non-transactional)
     */
    void publishDirect(DomainEventDTO event);
}
