package com.scmcloud.inventory.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Transactional outbox event for inventory CDC (template copy of order outbox).
 * <p>Same table {@code outbox_event}; written in same TX as inventory mutation,
 * relayed to Kafka topic {@code "scm." + aggregateType} by OutboxRelayJob.</p>
 */
@Data
@Accessors(chain = true)
@TableName("outbox_event")
public class OutboxEvent {

    @TableId("id")
    private String id;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("aggregate_type")
    private String aggregateType;

    @TableField("aggregate_id")
    private String aggregateId;

    @TableField("event_type")
    private String eventType;

    @TableField("payload")
    private String payload;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("published")
    private Boolean published;

    @TableField("published_at")
    private OffsetDateTime publishedAt;

    public static OutboxEvent of(UUID tenantId, String aggregateType, String aggregateId,
                                 String eventType, String payloadJson) {
        OutboxEvent e = new OutboxEvent();
        e.setId(UUID.randomUUID().toString());
        e.setTenantId(tenantId);
        e.setAggregateType(aggregateType);
        e.setAggregateId(aggregateId);
        e.setEventType(eventType);
        e.setPayload(payloadJson);
        e.setCreatedAt(OffsetDateTime.now());
        e.setPublished(false);
        return e;
    }
}
