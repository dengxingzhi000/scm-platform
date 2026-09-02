package com.scmcloud.common.integration.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.integration.messaging.KafkaMessagePublisher;
import com.scmcloud.common.integration.model.MessageEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock private OutboxService outboxService;
    @Mock private KafkaMessagePublisher kafkaPublisher;

    private OutboxPoller poller;

    @BeforeEach
    void setUp() {
        poller = new OutboxPoller(outboxService, kafkaPublisher, new ObjectMapper());
    }

    @Test
    void publishEventShouldUseDynamicTopicBasedOnAggregateType() {
        UUID tenantId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.create("ORDER_CREATED", "OrdOrder", "order-1", "{\"x\":1}", tenantId);
        when(kafkaPublisher.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        poller.publishEvent(event);

        verify(kafkaPublisher).send(eq("scm.ordorder"), eq("order-1"), any(MessageEnvelope.class));
    }

    @Test
    void publishEventShouldUseDynamicTopicForInventoryAggregateType() {
        UUID tenantId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.create("INVENTORY_ADJUSTED", "Inventory", "inv-1", "{}", tenantId);
        when(kafkaPublisher.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        poller.publishEvent(event);

        verify(kafkaPublisher).send(eq("scm.inventory"), eq("inv-1"), any(MessageEnvelope.class));
    }

    @Test
    void publishEventShouldMarkPublishedAfterKafkaAck() {
        UUID tenantId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.create("ORDER_CREATED", "OrdOrder", "order-1", "{}", tenantId);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaPublisher.send(any(), any(), any())).thenReturn(future);

        poller.publishEvent(event);

        verify(outboxService).markPublished(event.getId());
    }

    @Test
    void publishEventShouldMarkFailedWhenSendFutureFails() {
        UUID tenantId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.create("ORDER_CREATED", "OrdOrder", "order-1", "{}", tenantId);
        CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("kafka down"));
        when(kafkaPublisher.send(any(), any(), any())).thenReturn(failed);

        poller.publishEvent(event);

        verify(outboxService).markFailed(eq(event.getId()), any());
    }

    @Test
    void publishEventShouldBuildEnvelopeCarryingEventTypeAndTenant() {
        UUID tenantId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.create("ORDER_CREATED", "OrdOrder", "order-1",
                "{\"x\":1}", tenantId);
        when(kafkaPublisher.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        poller.publishEvent(event);

        ArgumentCaptor<MessageEnvelope> captor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(kafkaPublisher).send(any(), any(), captor.capture());
        MessageEnvelope env = captor.getValue();
        assertEquals("ORDER_CREATED", env.getType());
        assertEquals(tenantId.toString(), env.getTenantId());
    }
}