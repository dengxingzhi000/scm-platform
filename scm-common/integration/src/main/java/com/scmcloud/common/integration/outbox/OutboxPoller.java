package com.scmcloud.common.integration.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scmcloud.common.integration.messaging.KafkaMessagePublisher;
import com.scmcloud.common.integration.model.MessageEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Polls the outbox table for unpublished events and publishes to Kafka.
 * Runs on a fixed schedule. Uses SELECT ... FOR UPDATE SKIP LOCKED
 * to support multiple instances without duplicate publishing.
 */
@Slf4j
@Component
@ConditionalOnBean(KafkaMessagePublisher.class)
public class OutboxPoller {

    private final OutboxService outboxService;
    private final KafkaMessagePublisher kafkaPublisher;
    private final ObjectMapper objectMapper;

    public OutboxPoller(OutboxService outboxService,
                        KafkaMessagePublisher kafkaPublisher,
                        ObjectMapper objectMapper) {
        this.outboxService = outboxService;
        this.kafkaPublisher = kafkaPublisher;
        // Defensive copy + register JavaTimeModule so we can serialize
        // OutboxEvent (OffsetDateTime fields) even when the injected
        // ObjectMapper was created bare (e.g. by tests).
        this.objectMapper = objectMapper.copy().registerModule(new JavaTimeModule());
    }

    private static final int BATCH_SIZE = 50;
    private static final long KAFKA_ACK_TIMEOUT_SECONDS = 5;
    private static final String TOPIC_PREFIX = "scm.";

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:1000}")
    public void pollAndPublish() {
        // 1. Publish pending events
        List<OutboxEvent> pending = outboxService.findPendingEvents(BATCH_SIZE);
        for (OutboxEvent event : pending) {
            publishEvent(event);
        }

        // 2. Retry failed events
        List<OutboxEvent> retryable = outboxService.findRetryableEvents(BATCH_SIZE);
        for (OutboxEvent event : retryable) {
            publishEvent(event);
        }

        if (!pending.isEmpty() || !retryable.isEmpty()) {
            log.debug("Outbox: processed {} pending, {} retryable", pending.size(), retryable.size());
        }
    }

    void publishEvent(OutboxEvent event) {
        String topic = buildTopic(event.getAggregateType());
        try {
            // The envelope's `data` carries the full OutboxEvent JSON so that
            // downstream consumers can derive a STABLE deduplication key from
            // event.getId(). `MessageEnvelope.of()` regenerates envelope.id on
            // every call (it is NOT stable across retries), so the consumer
            // MUST NOT use envelope.id for dedup.
            String dataJson = objectMapper.writeValueAsString(event);
            MessageEnvelope<String> envelope = MessageEnvelope.of(
                    event.getEventType(),
                    "outbox",
                    dataJson
            ).toBuilder()
                    .tenantId(event.getTenantId() != null ? event.getTenantId().toString() : null)
                    .build();

            kafkaPublisher.send(topic, event.getAggregateId(), envelope)
                    .get(KAFKA_ACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            outboxService.markPublished(event.getId());
            log.debug("Published outbox event: type={}, topic={}, id={}",
                    event.getEventType(), topic, event.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while publishing outbox event id={}, topic={}: {}",
                    event.getId(), topic, e.getMessage());
            outboxService.markFailed(event.getId(), "interrupted: " + e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to publish outbox event id={}, topic={}: {}",
                    event.getId(), topic, e.getMessage());
            outboxService.markFailed(event.getId(), e.getMessage());
        }
    }

    private static String buildTopic(String aggregateType) {
        if (aggregateType == null || aggregateType.isBlank()) {
            return TOPIC_PREFIX + "events";
        }
        return TOPIC_PREFIX + aggregateType.toLowerCase();
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanup() {
        int deleted = outboxService.cleanup(7);
        if (deleted > 0) {
            log.info("Outbox cleanup: removed {} old published events", deleted);
        }
    }
}