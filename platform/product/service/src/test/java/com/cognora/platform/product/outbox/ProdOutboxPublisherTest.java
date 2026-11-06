package com.cognora.platform.product.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * outbox 发布侧：成功回填 published_at，失败只累加 retry_count 并保留可见，
 * 达到上限后不再投递。
 */
class ProdOutboxPublisherTest {

    private static final String TOPIC = "scm_product_server.public.prod_spu";
    private static final int MAX_RETRIES = 5;

    private ProdOutboxService outboxService;
    private KafkaTemplate<String, String> kafkaTemplate;
    private ProdOutboxPublisher publisher;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        outboxService = mock(ProdOutboxService.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        publisher = new ProdOutboxPublisher(outboxService, kafkaTemplate, TOPIC, 100, MAX_RETRIES, 5000);
    }

    private ProdOutboxEvent event(int retryCount) {
        return new ProdOutboxEvent()
                .setId("evt-1")
                .setTenantId("00000000-0000-0000-0000-00000000000a")
                .setAggregateId("spu-1")
                .setEventType(ProdOutboxService.EVENT_TYPE_SPU_UPSERT)
                .setPayload("{\"op\":\"u\",\"after\":{\"id\":\"spu-1\"}}")
                .setRetryCount(retryCount);
    }

    @Test
    void successfulSendBackfillsPublishedAt() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        assertTrue(publisher.publish(event(0)));

        verify(kafkaTemplate).send(TOPIC, "spu-1", "{\"op\":\"u\",\"after\":{\"id\":\"spu-1\"}}");
        verify(outboxService).markPublished("evt-1");
        verify(outboxService, never()).incrementRetryCount(anyString());
    }

    @Test
    void failedSendIncrementsRetryCountAndKeepsTheRowUnpublished() {
        CompletableFuture<SendResult<String, String>> failed =
                CompletableFuture.failedFuture(new RuntimeException("kafka unavailable"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failed);

        assertFalse(publisher.publish(event(0)));

        // 只累加计数，不回填 published_at —— 行仍然是待发布状态，下次轮询还会重试
        verify(outboxService).incrementRetryCount("evt-1");
        verify(outboxService, never()).markPublished(anyString());
    }

    @Test
    void whenKafkaSendThrowsSynchronouslyTheRowIsStillRetried() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("producer closed"));

        assertFalse(publisher.publish(event(0)));

        verify(outboxService).incrementRetryCount("evt-1");
        verify(outboxService, never()).markPublished(anyString());
    }

    @Test
    void rowsAtTheRetryCapAreNotSentAgain() {
        assertFalse(publisher.publish(event(MAX_RETRIES)));

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        verify(outboxService, never()).markPublished(anyString());
        verify(outboxService, never()).incrementRetryCount(anyString());
    }

    @Test
    void pollPublishesEveryRowAndQueriesOnlyUnpublishedOnesUnderTheCap() {
        when(outboxService.findPublishable(MAX_RETRIES, 100))
                .thenReturn(List.of(event(0), event(1)));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publishPending();

        verify(outboxService).findPublishable(MAX_RETRIES, 100);
        verify(outboxService, org.mockito.Mockito.times(2)).markPublished("evt-1");
    }

    @Test
    void pollStopsAfterConsecutiveFailuresSoKafkaOutageDoesNotHogTheScheduler() {
        when(outboxService.findPublishable(MAX_RETRIES, 100))
                .thenReturn(List.of(event(0), event(0), event(0), event(0), event(0)));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka unavailable")));

        publisher.publishPending();

        // 连续 3 条失败后结束本轮，剩余行留给下一次轮询
        verify(kafkaTemplate, org.mockito.Mockito.times(3))
                .send(anyString(), anyString(), anyString());
        verify(outboxService, org.mockito.Mockito.times(3)).incrementRetryCount("evt-1");
        verify(outboxService, never()).markPublished(anyString());
    }

    @Test
    void pollDoesNothingWhenTheOutboxIsEmpty() {
        when(outboxService.findPublishable(MAX_RETRIES, 100)).thenReturn(List.of());

        publisher.publishPending();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        verify(outboxService, never()).markPublished(anyString());
    }
}
