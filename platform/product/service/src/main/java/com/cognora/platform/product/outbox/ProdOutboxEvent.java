package com.cognora.platform.product.outbox;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;

/**
 * prod_outbox 的一行变更事件。
 *
 * <p>由 {@link ProdOutboxService} 在商品写入的同一事务内插入，由
 * {@link ProdOutboxPublisher} 定时投递到 Kafka 后回填 {@code published_at}。</p>
 */
@Data
@Accessors(chain = true)
@TableName("prod_outbox")
public class ProdOutboxEvent {

    @TableId("id")
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("aggregate_id")
    private String aggregateId;

    @TableField("event_type")
    private String eventType;

    /**
     * Debezium 兼容的事件体（{@code op} + {@code after}/{@code before}）。
     * 消费者 ProductSyncConsumer 直接按这个结构解析，因此这里存最终要发送的 JSON。
     */
    @TableField("payload")
    private String payload;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("published_at")
    private OffsetDateTime publishedAt;

    @TableField("retry_count")
    private Integer retryCount;
}
