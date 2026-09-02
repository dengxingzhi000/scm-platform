package com.scmcloud.common.integration.outbox;

import com.scmcloud.common.integration.outbox.OutboxService.OutboxEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock private OutboxEventMapper outboxEventMapper;

    private OutboxService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new OutboxService(outboxEventMapper, objectMapper);
    }

    @Test
    void explicitSaveShouldInsertOutboxEventWithProvidedAggregateType() {
        UUID tenantId = UUID.randomUUID();
        when(outboxEventMapper.insert(any(OutboxEvent.class))).thenReturn(1);

        service.save("ORDER_CREATED", "OrdOrder", "order-123",
                new TestPayload("test"), tenantId);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventMapper).insert(captor.capture());
        OutboxEvent event = captor.getValue();
        assertEquals("ORDER_CREATED", event.getEventType());
        assertEquals("OrdOrder", event.getAggregateType());
        assertEquals("order-123", event.getAggregateId());
        assertEquals("PENDING", event.getStatus());
        assertEquals(0, event.getRetryCount());
        assertEquals(5, event.getMaxRetries());
        assertEquals(tenantId, event.getTenantId());
        assertNotNull(event.getId());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void explicitSaveShouldSerializePayloadToJson() {
        UUID tenantId = UUID.randomUUID();
        when(outboxEventMapper.insert(any(OutboxEvent.class))).thenReturn(1);

        service.save("INVENTORY_ADJUSTED", "Inventory", "inv-1",
                new TestPayload("hello"), tenantId);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventMapper).insert(captor.capture());
        String payload = captor.getValue().getPayload();
        assertNotNull(payload);
        org.junit.jupiter.api.Assertions.assertTrue(payload.contains("hello"));
    }

    @Test
    void explicitSaveShouldPropagateExceptionWhenMapperThrows() {
        UUID tenantId = UUID.randomUUID();
        when(outboxEventMapper.insert(any(OutboxEvent.class)))
                .thenThrow(new org.springframework.dao.DataAccessException("DB error") {});

        assertThrows(RuntimeException.class,
                () -> service.save("ORDER_CREATED", "OrdOrder", "x",
                        new TestPayload("p"), tenantId));
    }

    public static class TestPayload {
        private final String value;
        public TestPayload(String value) { this.value = value; }
        public String getValue() { return value; }
    }
}