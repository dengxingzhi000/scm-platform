package com.scmcloud.analytics.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scmcloud.analytics.config.AnalyticsKafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
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
        String envelope = buildOrderCreatedEnvelope(tenant, orderNo);

        testProducer.send(OdsIngestConsumer.TOPIC_ORDER, orderNo, envelope).get(5, TimeUnit.SECONDS);
        testProducer.send(OdsIngestConsumer.TOPIC_ORDER, orderNo, envelope).get(5, TimeUnit.SECONDS);

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() ->
                        verify(odsWriter, atLeastOnce()).insertBatch(eq("ods_order"), any()));

        Thread.sleep(1500);
        verify(odsWriter, times(1)).insertBatch(eq("ods_order"), any());
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
