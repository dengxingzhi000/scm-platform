package com.scmcloud.common.integration.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.domain.event.DomainEvent;
import com.scmcloud.common.domain.event.DomainEventPublisher;
import com.scmcloud.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for writing domain events to the outbox table.
 * Events are written in the same DB transaction as the aggregate mutation.
 *
 * <p>Usage in a command service:</p>
 * <pre>
 * outboxService.save(orderCreatedEvent);
 * // ... entity mutation happens in the same @Transactional method
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    /**
     * Save a domain event to the outbox. Must be called within a @Transactional boundary.
     */
    public void save(DomainEvent event) {
        save(event.getEventType(), event.getClass().getSimpleName(), event.getEventId(), event, event.getTenantId());
    }

    /**
     * Save a domain event to the outbox with an explicit aggregate type.
     *
     * <p>Use this overload when the event does not extend {@link DomainEvent} or when
     * the desired {@code aggregateType} differs from the event class's simple name
     * (e.g. a generic event class routed to a specific aggregate).</p>
     *
     * <p>Must be called within a {@code @Transactional} boundary so the outbox row
     * is inserted in the same transaction as the aggregate mutation.</p>
     *
     * @param eventType      event type identifier (e.g. {@code "ORDER_CREATED"})
     * @param aggregateType  aggregate type used for topic routing (e.g. {@code "OrdOrder"})
     * @param aggregateId    aggregate id (e.g. order id)
     * @param payload        payload object; serialized to JSON
     * @param tenantId       tenant id
     */
    public void save(String eventType, String aggregateType, String aggregateId,
                     Object payload, UUID tenantId) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEvent outboxEvent = OutboxEvent.create(
                    eventType, aggregateType, aggregateId, payloadJson, tenantId
            );
            outboxEventMapper.insert(outboxEvent);
            log.debug("Saved domain event to outbox: type={}, aggregateType={}, id={}",
                    eventType, aggregateType, outboxEvent.getId());
        } catch (Exception e) {
            log.error("Failed to save domain event to outbox: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save domain event to outbox", e);
        }
    }

    /**
     * Fetch pending events for publishing.
     */
    public List<OutboxEvent> findPendingEvents(int batchSize) {
        return outboxEventMapper.findPending(batchSize);
    }

    /**
     * Fetch failed events eligible for retry.
     */
    public List<OutboxEvent> findRetryableEvents(int batchSize) {
        return outboxEventMapper.findRetryable(batchSize);
    }

    /**
     * Mark an event as published.
     */
    public void markPublished(String eventId) {
        outboxEventMapper.markPublished(eventId);
    }

    /**
     * Mark an event as failed with error message.
     */
    public void markFailed(String eventId, String error) {
        outboxEventMapper.markFailed(eventId, error);
    }

    /**
     * Cleanup old published events (retain for 7 days).
     */
    public int cleanup(int retentionDays) {
        return outboxEventMapper.deleteOldPublished(retentionDays);
    }

    @Mapper
    public interface OutboxEventMapper {

        @Insert("INSERT INTO outbox_event (id, event_type, aggregate_type, aggregate_id, payload, tenant_id, " +
                "status, retry_count, max_retries, created_at) " +
                "VALUES (#{id}, #{eventType}, #{aggregateType}, #{aggregateId}, #{payload}::jsonb, #{tenantId}, " +
                "#{status}, #{retryCount}, #{maxRetries}, #{createdAt})")
        int insert(OutboxEvent event);

        @Select("SELECT * FROM outbox_event WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT #{limit} FOR UPDATE SKIP LOCKED")
        List<OutboxEvent> findPending(@Param("limit") int limit);

        @Select("SELECT * FROM outbox_event WHERE status = 'FAILED' AND retry_count < max_retries " +
                "AND next_retry_at <= CURRENT_TIMESTAMP ORDER BY next_retry_at ASC LIMIT #{limit} FOR UPDATE SKIP LOCKED")
        List<OutboxEvent> findRetryable(@Param("limit") int limit);

        @Update("UPDATE outbox_event SET status = 'PUBLISHED', published_at = CURRENT_TIMESTAMP WHERE id = #{eventId}")
        int markPublished(@Param("eventId") String eventId);

        @Update("UPDATE outbox_event SET status = 'FAILED', last_error = #{error}, " +
                "retry_count = retry_count + 1, " +
                "next_retry_at = CURRENT_TIMESTAMP + (2 ^ LEAST(retry_count + 1, 5)) * INTERVAL '1 second' " +
                "WHERE id = #{eventId}")
        int markFailed(@Param("eventId") String eventId, @Param("error") String error);

        @Update("DELETE FROM outbox_event WHERE status = 'PUBLISHED' " +
                "AND published_at < CURRENT_TIMESTAMP - (#{days} || ' days')::interval")
        int deleteOldPublished(@Param("days") int days);
    }
}
