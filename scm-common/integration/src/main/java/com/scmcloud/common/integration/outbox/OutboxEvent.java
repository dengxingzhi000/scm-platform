package com.scmcloud.common.integration.outbox;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Outbox event entity. Written in the same DB transaction as the aggregate,
 * then polled and published to Kafka by the OutboxPoller.
 */
@Data
@TableName("outbox_event")
public class OutboxEvent {

    @TableId("id")
    private String id;

    @TableField("event_type")
    private String eventType;

    @TableField("aggregate_type")
    private String aggregateType;

    @TableField("aggregate_id")
    private String aggregateId;

    @TableField("payload")
    private String payload;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private UUID tenantId;

    @TableField("status")
    private String status;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("max_retries")
    private Integer maxRetries;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("published_at")
    private OffsetDateTime publishedAt;

    @TableField("last_error")
    private String lastError;

    @TableField("next_retry_at")
    private OffsetDateTime nextRetryAt;

    public static OutboxEvent create(String eventType, String aggregateType, String aggregateId,
                                     String payload, UUID tenantId) {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setPayload(payload);
        event.setTenantId(tenantId);
        event.setStatus("PENDING");
        event.setRetryCount(0);
        event.setMaxRetries(5);
        event.setCreatedAt(OffsetDateTime.now());
        return event;
    }

    public void markPublished() {
        this.status = "PUBLISHED";
        this.publishedAt = OffsetDateTime.now();
    }

    public void markFailed(String error) {
        this.status = "FAILED";
        this.lastError = error;
        this.retryCount++;
        // Exponential backoff: 1s, 2s, 4s, 8s, 16s
        long delaySeconds = (long) Math.pow(2, retryCount);
        this.nextRetryAt = OffsetDateTime.now().plusSeconds(delaySeconds);
    }

    @JsonIgnore
    public boolean isRetryable() {
        return "FAILED".equals(status) && retryCount < maxRetries;
    }

    @JsonIgnore
    public boolean isPending() {
        return "PENDING".equals(status);
    }
}
