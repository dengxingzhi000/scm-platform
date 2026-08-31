package com.scmcloud.analytics.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Transactional outbox relay for analytics CDC.
 * <p>Polls business outbox tables (order/inventory) and publishes to Kafka topic
 * {@code "scm." + aggregateType.toLowerCase()} then marks {@code published=true}.
 * Uses simple polling; for multi-instance safety add {@code @DistributedLock}
 * or rely on {@code FOR UPDATE SKIP LOCKED} in the mapper query.</p>
 *
 * <p>Template: copy to each business service DB or centralize in analytics where
 * it has read access to business DBs via additional DataSource or shared table.</p>
 *
 * <p>For now this component operates on the analytics DB's {@code outbox_event}
 * table. In production, wire separate DataSources per business DB or use Debezium.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayJob {

    private final OutboxEventMapper outboxEventMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 100;

    // Uncomment to enforce single-instance execution via Redis lock:
    // @DistributedLockAnnotation(key = "'outbox:relay'", ttl = 30)
    @Scheduled(fixedDelay = 5000)
    public void relay() {
        List<OutboxEvent> events = outboxEventMapper.findUnpublished(BATCH_SIZE);
        if (events.isEmpty()) {
            return;
        }
        log.info("OutboxRelay: polling {} unpublished events", events.size());
        for (OutboxEvent event : events) {
            try {
                String topic = "scm." + event.getAggregateType().toLowerCase();
                // payload already JSON string
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload());
                outboxEventMapper.markPublished(event.getId());
                log.debug("OutboxRelay published: id={}, topic={}, aggregate={}/{}",
                        event.getId(), topic, event.getAggregateType(), event.getAggregateId());
            } catch (Exception e) {
                log.warn("OutboxRelay failed to publish id={}: {}", event.getId(), e.getMessage(), e);
                // leave published=false for retry on next poll; optionally add retry_count
            }
        }
    }

    // ——— Mapper & Entity inline for analytics relay ———
    // Kept in same file to avoid extra entity package for this demo relay.
    // Order/inventory services have their own OutboxEvent + OutboxMapper copies (template).

    public static class OutboxEvent {
        private String id;
        private UUID tenantId;
        private String aggregateType;
        private String aggregateId;
        private String eventType;
        private String payload;
        private OffsetDateTime createdAt;
        private Boolean published;
        private OffsetDateTime publishedAt;

        // getters/setters for MyBatis
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
        public String getAggregateType() { return aggregateType; }
        public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
        public String getAggregateId() { return aggregateId; }
        public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public Boolean getPublished() { return published; }
        public void setPublished(Boolean published) { this.published = published; }
        public OffsetDateTime getPublishedAt() { return publishedAt; }
        public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
    }

    @Mapper
    public interface OutboxEventMapper {
        @Select("SELECT id, tenant_id, aggregate_type, aggregate_id, event_type, payload, created_at, published, published_at "
                + "FROM outbox_event WHERE published = false ORDER BY created_at ASC LIMIT #{limit} FOR UPDATE SKIP LOCKED")
        List<OutboxEvent> findUnpublished(@Param("limit") int limit);

        @Update("UPDATE outbox_event SET published = true, published_at = now() WHERE id = #{id}")
        int markPublished(@Param("id") String id);
    }
}
