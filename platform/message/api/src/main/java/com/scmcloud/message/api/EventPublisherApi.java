package com.scmcloud.message.api;

import com.scmcloud.message.api.dto.DomainEventDTO;

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
