package com.cognora.platform.product.outbox;

import com.cognora.platform.product.domain.entity.ProdSpu;
import com.cognora.platform.product.mapper.ProdOutboxMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.cognora.platform.common.tenant.TenantContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * outbox 写入侧：事件体必须是 Debezium 兼容结构、必须带 tenant_id，
 * upsert/delete 语义与消费者现有的解析逻辑一致。
 */
class ProdOutboxServiceTest {

    private ProdOutboxMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mapper = mock(ProdOutboxMapper.class);
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private ProdOutboxEvent captureInsert() {
        ArgumentCaptor<ProdOutboxEvent> captor = ArgumentCaptor.forClass(ProdOutboxEvent.class);
        verify(mapper).insert(captor.capture());
        return captor.getValue();
    }

    private JsonNode payloadOf(ProdOutboxEvent row) throws Exception {
        return objectMapper.readTree(row.getPayload());
    }

    private ProdSpu spu() {
        ProdSpu spu = new ProdSpu();
        spu.setId("spu-1");
        spu.setTenantId("00000000-0000-0000-0000-00000000000a");
        spu.setSpuCode("SPU-1");
        spu.setSpuName("Phone");
        spu.setCategoryId("cat-1");
        spu.setBrandId("brand-1");
        spu.setDescription("desc");
        spu.setMainImage("https://img/1.png");
        spu.setMinPrice(new BigDecimal("99.90"));
        spu.setMaxPrice(new BigDecimal("199.90"));
        spu.setTotalStock(10);
        spu.setTotalSales(3);
        spu.setStatus(1);
        spu.setSortOrder(1);
        spu.setCreateTime(LocalDateTime.of(2026, 1, 1, 8, 0));
        spu.setUpdateTime(LocalDateTime.of(2026, 1, 2, 9, 0));
        return spu;
    }

    @Test
    void createWritesUpsertEventWithTenantIdAndConsumerFieldNames() throws Exception {
        ProdOutboxService service = new ProdOutboxService(mapper, objectMapper);
        service.recordSpuUpsert(spu(), true);

        ProdOutboxEvent row = captureInsert();
        assertEquals(ProdOutboxService.EVENT_TYPE_SPU_UPSERT, row.getEventType());
        assertEquals("spu-1", row.getAggregateId());
        assertEquals("00000000-0000-0000-0000-00000000000a", row.getTenantId());
        assertEquals(0, row.getRetryCount());

        JsonNode payload = payloadOf(row);
        assertEquals("c", payload.path("op").asText());
        // 消费者按这些字段名取值；缺 tenant_id 会被直接拒写
        assertEquals("00000000-0000-0000-0000-00000000000a", payload.path("after").path("tenant_id").asText());
        assertEquals("SPU-1", payload.path("after").path("spu_code").asText());
        assertEquals("Phone", payload.path("after").path("spu_name").asText());
        assertEquals("99.90", payload.path("after").path("min_price").asText());
        assertEquals("199.90", payload.path("after").path("max_price").asText());
        assertEquals(10, payload.path("after").path("total_stock").asInt());
        assertEquals(1, payload.path("after").path("status").asInt());
    }

    @Test
    void timeFieldsAreEpochMicrosSoTheConsumerCanRoundTripThem() throws Exception {
        ProdOutboxService service = new ProdOutboxService(mapper, objectMapper);
        service.recordSpuUpsert(spu(), true);

        JsonNode after = payloadOf(captureInsert()).path("after");
        long micros = after.path("create_time").asLong();
        LocalDateTime restored = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(micros / 1000), ZoneId.systemDefault());
        assertEquals(LocalDateTime.of(2026, 1, 1, 8, 0), restored);
    }

    @Test
    void updateUsesDebeziumUpdateOperation() throws Exception {
        ProdOutboxService service = new ProdOutboxService(mapper, objectMapper);
        service.recordSpuUpsert(spu(), false);

        JsonNode payload = payloadOf(captureInsert());
        assertEquals("u", payload.path("op").asText());
        assertTrue(payload.path("after").has("tenant_id"));
    }

    @Test
    void deleteEmitsBeforeOnlyDeleteEvent() throws Exception {
        ProdOutboxService service = new ProdOutboxService(mapper, objectMapper);
        service.recordSpuDelete(spu());

        ProdOutboxEvent row = captureInsert();
        assertEquals(ProdOutboxService.EVENT_TYPE_SPU_DELETE, row.getEventType());
        assertEquals("spu-1", row.getAggregateId());

        JsonNode payload = payloadOf(row);
        assertEquals("d", payload.path("op").asText());
        assertEquals("spu-1", payload.path("before").path("id").asText());
        assertEquals("00000000-0000-0000-0000-00000000000a",
                payload.path("before").path("tenant_id").asText());
        // 消费者删除分支只读 before，不能靠 after 兜底
        assertFalse(payload.has("after"));
    }

    @Test
    void missingTenantIdFailsClosedInsteadOfWritingAnUnusableEvent() {
        ProdOutboxService service = new ProdOutboxService(mapper, objectMapper);
        ProdSpu spu = spu();
        spu.setTenantId(null);

        assertThrows(IllegalStateException.class, () -> service.recordSpuUpsert(spu, true));
        verify(mapper, never()).insert(any(ProdOutboxEvent.class));
    }

    @Test
    void tenantIdFallsBackToTheRequestContext() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        ProdOutboxService service = new ProdOutboxService(mapper, objectMapper);
        ProdSpu spu = spu();
        spu.setTenantId(null);
        service.recordSpuUpsert(spu, true);

        assertEquals(tenantId.toString(), captureInsert().getTenantId());
    }

    @Test
    void serializationFailurePropagatesSoTheBusinessTransactionRollsBack() throws Exception {
        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        when(brokenMapper.writeValueAsString(any())).thenThrow(new IllegalStateException("boom"));

        ProdOutboxService service = new ProdOutboxService(mapper, brokenMapper);

        assertThrows(IllegalStateException.class, () -> service.recordSpuUpsert(spu(), true));
        verify(mapper, never()).insert(any(ProdOutboxEvent.class));
    }
}
