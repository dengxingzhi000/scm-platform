package com.scmcloud.analytics.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scmcloud.analytics.config.AnalyticsKafkaConfig;
import com.scmcloud.common.integration.outbox.OutboxEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(classes = OdsIngestConsumerTest.TestConfig.class)
@EmbeddedKafka(topics = {"scm.ordorder", "scm.inventory"}, partitions = 1)
@TestPropertySource(properties = {
        "clickhouse.url=http://localhost:8123",
        "clickhouse.database=analytics",
        "clickhouse.username=default",
        "clickhouse.password=",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=analytics-ods-test",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "analytics.ods.flush-interval-ms=200"
})
class OdsIngestConsumerTest {

    @Autowired
    KafkaTemplate<String, String> testProducer;

    @Autowired
    ClickHouseOdsWriter odsWriter;

    @Autowired
    OdsIngestConsumer consumer;

    /**
     * The Spring context is shared across test methods, so we reset the writer
     * mock and any consumer-side circuit-break state after each test to keep
     * them isolated.
     */
    @AfterEach
    void resetState() {
        reset(odsWriter);
        consumer.resetCircuitForTest();
    }

    @Test
    void consumeOrderCreatedRoutesRowToOdsOrder() throws Exception {
        String tenant = "tenant-" + UUID.randomUUID();
        String orderNo = "ORD-" + UUID.randomUUID();
        String envelope = buildOrderCreatedEnvelope(tenant, orderNo);

        testProducer.send(OdsIngestConsumer.TOPIC_ORDER, orderNo, envelope).get(5, TimeUnit.SECONDS);

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> verify(odsWriter, atLeastOnce()).insertBatch(
                        eq("ods_order"),
                        argThat(rows -> rows != null && rows.stream().anyMatch(r ->
                                tenant.equals(String.valueOf(r.get("tenant_id")))
                                        && orderNo.equals(String.valueOf(r.get("order_no")))
                        ))));
    }

    @Test
    void duplicateEnvelopeIdIsDeduped() throws Exception {
        String tenant = "tenant-" + UUID.randomUUID();
        String orderNo = "ORD-DUP-" + UUID.randomUUID();
        String stableEventId = "stable-" + UUID.randomUUID();

        // Send two envelopes with DIFFERENT envelope.id but the SAME inner OutboxEvent.id.
        // OutboxPoller regenerates envelope.id per publish (retry/republish), so the
        // dedup key MUST come from the inner OutboxEvent.id, not from envelope.id.
        String envelope1 = buildOutboxEventEnvelope(tenant, orderNo, stableEventId, "env-" + UUID.randomUUID());
        String envelope2 = buildOutboxEventEnvelope(tenant, orderNo, stableEventId, "env-" + UUID.randomUUID());

        testProducer.send(OdsIngestConsumer.TOPIC_ORDER, orderNo, envelope1).get(5, TimeUnit.SECONDS);
        testProducer.send(OdsIngestConsumer.TOPIC_ORDER, orderNo, envelope2).get(5, TimeUnit.SECONDS);

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() ->
                        verify(odsWriter, atLeastOnce()).insertBatch(eq("ods_order"), any()));

        Thread.sleep(1500);
        verify(odsWriter, times(1)).insertBatch(eq("ods_order"), any());
    }

    @Test
    void flushFailureReEnqueuesRows() throws Exception {
        String tenant = "tenant-" + UUID.randomUUID();
        String orderNo = "ORD-FAIL-" + UUID.randomUUID();
        String envelope = buildOrderCreatedEnvelope(tenant, orderNo);

        // Configure writer to throw for the upcoming flush attempt.
        doThrow(new RuntimeException("ClickHouse down"))
                .when(odsWriter).insertBatch(anyString(), any());

        testProducer.send(OdsIngestConsumer.TOPIC_ORDER, orderNo, envelope).get(5, TimeUnit.SECONDS);

        // The row should be in the queue (handleSingle succeeds regardless of writer health).
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertTrue(consumer.pendingCount() > 0,
                        "Row must reach the pending queue before the flush failure is exercised"));

        // Trigger a manual flush. With the fix, the failed batch is re-enqueued at
        // the front of the queue (preserving order). Without the fix, the catch
        // block silently drops the drained rows.
        consumer.flushNowForTest();

        assertTrue(consumer.pendingCount() > 0,
                "Failed flush should re-enqueue rows at the front of the queue");
    }

    @Test
    void boundedQueueRejectsPastCapacity() throws Exception {
        ClickHouseOdsWriter writer = mock(ClickHouseOdsWriter.class);
        // Tiny queue capacity to exercise the bound without sending 10k records.
        OdsIngestConsumer smallConsumer = new OdsIngestConsumer(
                writer, new InMemoryOdsDedupService(), new ObjectMapper(), 2);

        assertEquals(0, smallConsumer.pendingCount());

        OdsRow r1 = new OdsRow("ods_order", new HashMap<>());
        OdsRow r2 = new OdsRow("ods_order", new HashMap<>());
        OdsRow r3 = new OdsRow("ods_order", new HashMap<>());

        assertTrue(smallConsumer.offerNowForTest(r1), "First offer should succeed");
        assertTrue(smallConsumer.offerNowForTest(r2), "Second offer should succeed (queue at capacity)");
        assertFalse(smallConsumer.offerNowForTest(r3),
                "Third offer must be rejected (queue full — backpressure to Kafka)");
        assertEquals(2, smallConsumer.pendingCount(), "Queue must not exceed capacity");
    }

    @Test
    void consumeInventoryAdjustedRoutesRowToOdsInventory() throws Exception {
        String tenant = "tenant-" + UUID.randomUUID();
        String envelope = buildInventoryAdjustedEnvelope(tenant, "SKU-1", "WH-1");

        testProducer.send(OdsIngestConsumer.TOPIC_INVENTORY, "inv-key", envelope).get(5, TimeUnit.SECONDS);

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> verify(odsWriter, atLeastOnce()).insertBatch(
                        eq("ods_inventory"),
                        argThat(rows -> rows != null && rows.stream().anyMatch(r ->
                                tenant.equals(String.valueOf(r.get("tenant_id")))
                                        && "SKU-1".equals(String.valueOf(r.get("sku_id")))
                                        && "WH-1".equals(String.valueOf(r.get("warehouse_id")))
                        ))));
    }

    private static String buildOrderCreatedEnvelope(String tenantId, String orderNo) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode inner = mapper.createObjectNode();
        inner.put("eventId", UUID.randomUUID().toString());
        inner.put("tenantId", tenantId);
        inner.put("orderId", UUID.randomUUID().toString());
        inner.put("orderNo", orderNo);
        inner.put("eventType", "ORDER_CREATED");
        inner.put("userId", "user-1");
        inner.put("totalAmount", 99.99);
        inner.put("payableAmount", 99.99);

        ObjectNode root = mapper.createObjectNode();
        root.put("id", "env-" + UUID.randomUUID());
        root.put("type", "ORDER_CREATED");
        root.put("source", "outbox");
        root.put("tenantId", tenantId);
        root.set("data", inner);
        return mapper.writeValueAsString(root);
    }

    private static String buildInventoryAdjustedEnvelope(String tenantId, String skuId, String warehouseId)
            throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode inner = mapper.createObjectNode();
        inner.put("tenantId", tenantId);
        inner.put("skuId", skuId);
        inner.put("warehouseId", warehouseId);
        inner.put("quantity", 5);
        inner.put("availableStock", 100);

        ObjectNode root = mapper.createObjectNode();
        root.put("id", "env-" + UUID.randomUUID());
        root.put("type", "INVENTORY_ADJUSTED");
        root.put("source", "outbox");
        root.put("tenantId", tenantId);
        root.set("data", inner);
        return mapper.writeValueAsString(root);
    }

    /**
     * Build an envelope whose {@code data} field is the JSON-serialized
     * {@link OutboxEvent} (matching the wire format produced by
     * {@code OutboxPoller.publishEvent}). The {@code outboxEventId} is the
     * STABLE inner event id used for dedup; {@code envelopeId} is the
     * per-publish envelope id regenerated by {@code MessageEnvelope.of()}.
     */
    private static String buildOutboxEventEnvelope(String tenantId, String orderNo,
                                                   String outboxEventId, String envelopeId) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        OutboxEvent outbox = new OutboxEvent();
        outbox.setId(outboxEventId);
        outbox.setEventType("ORDER_CREATED");
        outbox.setAggregateType("ordorder");
        outbox.setAggregateId(orderNo);
        outbox.setPayload(
                "{\"eventId\":\"" + UUID.randomUUID() + "\","
                        + "\"tenantId\":\"" + tenantId + "\","
                        + "\"orderNo\":\"" + orderNo + "\","
                        + "\"userId\":\"user-1\","
                        + "\"totalAmount\":99.99,"
                        + "\"payableAmount\":99.99}");
        outbox.setStatus("PUBLISHED");
        String innerJson = mapper.writeValueAsString(outbox);

        ObjectNode root = mapper.createObjectNode();
        root.put("id", envelopeId);
        root.put("type", "ORDER_CREATED");
        root.put("source", "outbox");
        root.put("tenantId", tenantId);
        root.put("data", innerJson);
        return mapper.writeValueAsString(root);
    }

    @Configuration
    @EnableKafka
    @EnableScheduling
    @Import(AnalyticsKafkaConfig.class)
    static class TestConfig {

        @Bean
        KafkaProperties kafkaProperties() {
            return new KafkaProperties();
        }

        @Bean
        ConsumerFactory<String, String> testConsumerFactory(EmbeddedKafkaBroker broker) {
            Map<String, Object> props = new HashMap<>();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "analytics-ods-test");
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            return new DefaultKafkaConsumerFactory<>(props);
        }

        @Bean
        ConcurrentKafkaListenerContainerFactory<String, String> odsKafkaListenerContainerFactory(
                ConsumerFactory<String, String> testConsumerFactory) {
            ConcurrentKafkaListenerContainerFactory<String, String> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(testConsumerFactory);
            factory.setBatchListener(true);
            factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
            return factory;
        }

        @Bean
        ProducerFactory<String, String> testProducerFactory(EmbeddedKafkaBroker broker) {
            Map<String, Object> props = new HashMap<>();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            return new DefaultKafkaProducerFactory<>(props);
        }

        @Bean
        KafkaTemplate<String, String> testKafkaTemplate(ProducerFactory<String, String> testProducerFactory) {
            return new KafkaTemplate<>(testProducerFactory);
        }

        @Bean
        OdsDedupService odsDedupService() {
            return new InMemoryOdsDedupService();
        }

        @Bean
        ClickHouseOdsWriter clickHouseOdsWriter() {
            return mock(ClickHouseOdsWriter.class);
        }

        @Bean
        OdsIngestConsumer odsIngestConsumer(ClickHouseOdsWriter writer, OdsDedupService dedup) {
            return new OdsIngestConsumer(writer, dedup, new ObjectMapper());
        }
    }

    static class InMemoryOdsDedupService implements OdsDedupService {
        private final ConcurrentHashMap<String, Boolean> seen = new ConcurrentHashMap<>();

        @Override
        public boolean tryAcquire(String eventId) {
            return seen.putIfAbsent(eventId, Boolean.TRUE) == null;
        }
    }
}
