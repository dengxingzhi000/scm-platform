package com.cognora.platform.product.outbox;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.product.domain.entity.ProdSpu;
import com.cognora.platform.product.mapper.ProdOutboxMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 商品变更事件的 outbox 写入与投递状态维护。
 *
 * <p>写入必须在商品业务写入的 {@code @Transactional} 边界内调用：插入与业务变更
 * 用同一个连接、同一个事务，业务回滚时事件行一起回滚。</p>
 *
 * <p>payload 刻意保持 Debezium 的 {@code {op, after/before}} 结构，所以
 * {@code ProductSyncConsumer} 不需要任何改动。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProdOutboxService {

    public static final String EVENT_TYPE_SPU_UPSERT = "SPU_UPSERT";
    public static final String EVENT_TYPE_SPU_DELETE = "SPU_DELETE";

    private final ProdOutboxMapper prodOutboxMapper;
    private final ObjectMapper objectMapper;

    /**
     * 记录一次 SPU 新增/更新。{@code created=true} 时 op 为 c，否则为 u。
     */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void recordSpuUpsert(ProdSpu spu, boolean created) {
        String tenantId = requireTenantId(spu);
        Map<String, Object> after = spuRow(spu);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("op", created ? "c" : "u");
        event.put("after", after);
        insert(EVENT_TYPE_SPU_UPSERT, spu.getId(), tenantId, event);
    }

    /**
     * 记录一次 SPU 删除。消费者只读取 before.id 与 before.tenant_id。
     */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void recordSpuDelete(ProdSpu spu) {
        String tenantId = requireTenantId(spu);
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("id", spu.getId());
        before.put("tenant_id", tenantId);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("op", "d");
        event.put("before", before);
        insert(EVENT_TYPE_SPU_DELETE, spu.getId(), tenantId, event);
    }

    public List<ProdOutboxEvent> findPublishable(int maxRetries, int batchSize) {
        return prodOutboxMapper.findPublishable(maxRetries, batchSize);
    }

    public void markPublished(String id) {
        prodOutboxMapper.markPublished(id);
    }

    public void incrementRetryCount(String id) {
        prodOutboxMapper.incrementRetryCount(id);
    }

    public long countUnpublished() {
        return prodOutboxMapper.countUnpublished();
    }

    private void insert(String eventType, String aggregateId, String tenantId, Map<String, Object> event) {
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            // 序列化失败必须让业务事务一起回滚，不能写进一条消费者无法解析的事件。
            throw new IllegalStateException(
                    "Failed to serialize product outbox event for spu " + aggregateId, e);
        }

        ProdOutboxEvent row = new ProdOutboxEvent()
                .setId(UUID.randomUUID().toString())
                .setTenantId(tenantId)
                .setAggregateId(aggregateId)
                .setEventType(eventType)
                .setPayload(payload)
                .setCreatedAt(OffsetDateTime.now())
                .setRetryCount(0);
        prodOutboxMapper.insert(row);
        log.debug("记录商品 outbox 事件: type={}, spuId={}, tenantId={}", eventType, aggregateId, tenantId);
    }

    /**
     * tenant_id 不能为空：消费者对缺失 tenant_id 的事件直接拒写，写进去只会堆积脏数据。
     * 实体字段没填时回退到当前租户上下文，两者都没有就失败并回滚业务事务。
     */
    private String requireTenantId(ProdSpu spu) {
        if (StringUtils.hasText(spu.getTenantId())) {
            return spu.getTenantId();
        }
        UUID fromContext = TenantContextHolder.getTenantId();
        if (fromContext == null) {
            throw new IllegalStateException(
                    "Cannot record product outbox event without tenant_id, spu=" + spu.getId());
        }
        return fromContext.toString();
    }

    /**
     * 按 ProductSyncConsumer 读取的字段名构造 after 行。
     *
     * <p>时间字段按 Debezium 约定写成「纪元微秒」；消费者用
     * {@code ZoneId.systemDefault()} 还原，这里用同一时区换算，保证往返一致。</p>
     */
    static Map<String, Object> spuRow(ProdSpu spu) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", spu.getId());
        row.put("tenant_id", spu.getTenantId());
        row.put("spu_code", spu.getSpuCode());
        row.put("spu_name", spu.getSpuName());
        row.put("category_id", spu.getCategoryId());
        row.put("brand_id", spu.getBrandId());
        row.put("description", spu.getDescription());
        row.put("main_image", spu.getMainImage());
        row.put("min_price", decimal(spu.getMinPrice()));
        row.put("max_price", decimal(spu.getMaxPrice()));
        row.put("total_stock", spu.getTotalStock());
        row.put("total_sales", spu.getTotalSales());
        row.put("seo_title", spu.getSeoTitle());
        row.put("seo_keywords", spu.getSeoKeywords());
        row.put("seo_description", spu.getSeoDescription());
        row.put("status", spu.getStatus());
        row.put("sort_order", spu.getSortOrder());
        row.put("published_at", epochMicros(spu.getPublishedAt()));
        row.put("create_time", epochMicros(spu.getCreateTime()));
        row.put("update_time", epochMicros(spu.getUpdateTime()));
        return row;
    }

    private static String decimal(java.math.BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private static Long epochMicros(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() * 1000L;
    }
}
