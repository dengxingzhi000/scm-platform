package com.scmcloud.analytics.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Subscribes to business CDC topics published by the outbox poller
 * ({@code scm.ordorder}, {@code scm.inventory}) and feeds transformed rows
 * into a thread-safe queue drained by {@link #flushPending()}.
 *
 * <p>Design notes:
 * <ul>
 *   <li>Batch listener — one call delivers {@code List<ConsumerRecord>}; we
 *       ack the whole batch at the end so a partial parse error can fail the
 *       batch and trigger redelivery.</li>
 *   <li>Dedup at envelope granularity via Redis SETNX (24h TTL).</li>
 *   <li>The queue + scheduled flush are owned by the consumer so
 *       {@link ClickHouseOdsWriter} stays a single-responsibility boundary and
 *       is straightforward to mock in tests.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OdsIngestConsumer {

    public static final int FLUSH_BATCH_SIZE = 1000;
    public static final String TOPIC_ORDER = "scm.ordorder";
    public static final String TOPIC_INVENTORY = "scm.inventory";

    private final ClickHouseOdsWriter writer;
    private final OdsDedupService dedupService;
    private final ObjectMapper objectMapper;

    private final BlockingQueue<OdsRow> pendingRows = new LinkedBlockingQueue<>();

    @KafkaListener(
            topics = {TOPIC_ORDER, TOPIC_INVENTORY},
            groupId = "analytics-ods",
            containerFactory = "odsKafkaListenerContainerFactory")
    public void consume(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        log.debug("consume() called with {} records", records == null ? 0 : records.size());
        if (records == null || records.isEmpty()) {
            ack.acknowledge();
            return;
        }

        for (ConsumerRecord<String, String> record : records) {
            handleSingle(record);
        }
        ack.acknowledge();
    }

    private void handleSingle(ConsumerRecord<String, String> record) {
        try {
            String envelopeJson = record.value();
            if (envelopeJson == null || envelopeJson.isEmpty()) {
                log.debug("Skipping empty record at offset {}", record.offset());
                return;
            }

            JsonNode envelope = objectMapper.readTree(envelopeJson);
            String envelopeId = textOrNull(envelope, "id");
            if (!dedupService.tryAcquire(envelopeId)) {
                return;
            }

            String tenantId = textOrNull(envelope, "tenantId");
            setTenantContext(tenantId);

            JsonNode dataNode = envelope.get("data");
            JsonNode inner = dataNode;
            // The Outbox poller sets `data` as the raw outbox row payload,
            // which is itself a JSON-encoded string. When it's already an
            // object node (some test producers), use it directly.
            if (dataNode != null && dataNode.isTextual()) {
                inner = objectMapper.readTree(dataNode.asText());
            }
            if (inner == null || inner.isMissingNode()) {
                log.warn("Skipping envelope with no inner payload: id={}", envelopeId);
                return;
            }

            OdsRow row = switch (record.topic()) {
                case TOPIC_ORDER -> OdsRowMapper.orderFromPayload(
                        record.topic(), inner, record.offset(), record.timestamp());
                case TOPIC_INVENTORY -> OdsRowMapper.inventoryFromPayload(
                        record.topic(), inner, record.offset(), record.timestamp());
                default -> {
                    log.debug("Ignoring topic {} for envelope {}", record.topic(), envelopeId);
                    yield null;
                }
            };
            if (row != null) {
                pendingRows.add(row);
            }
        } catch (Exception ex) {
            // Surface as a runtime exception so Kafka redelivers the batch
            log.warn("Failed to process record at offset {}: {}", record.offset(), ex.getMessage());
            throw new RuntimeException(ex);
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * Periodic flush — runs independently of Kafka cadence (every 5s by default,
     * overridable via {@code analytics.ods.flush-interval-ms} when added to the
     * configuration). Drains up to {@link #FLUSH_BATCH_SIZE} rows, groups by
     * table, and bulk-inserts each group.
     */
    @Scheduled(fixedDelayString = "${analytics.ods.flush-interval-ms:5000}")
    public void flushPending() {
        if (pendingRows.isEmpty()) {
            return;
        }
        log.debug("flushPending() draining {} rows", pendingRows.size());
        Map<String, List<Map<String, Object>>> byTable = new HashMap<>();
        List<OdsRow> drained = new ArrayList<>(FLUSH_BATCH_SIZE);
        pendingRows.drainTo(drained, FLUSH_BATCH_SIZE);
        for (OdsRow row : drained) {
            byTable.computeIfAbsent(row.getTable(), t -> new ArrayList<>()).add(row.getFields());
        }
        byTable.forEach((table, rows) -> {
            try {
                writer.insertBatch(table, rows);
            } catch (Exception ex) {
                log.warn("Flush failed for {} rows into {}: {}", rows.size(), table, ex.getMessage());
            }
        });
    }

    /**
     * Visible for tests — number of rows currently buffered.
     */
    int pendingCount() {
        return pendingRows.size();
    }

    /**
     * Visible for tests — manual drain trigger that bypasses the schedule.
     */
    void flushNowForTest() {
        flushPending();
    }

    private static void setTenantContext(String tenantIdStr) {
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            return;
        }
        try {
            TenantContextHolder.setTenantId(UUID.fromString(tenantIdStr));
        } catch (IllegalArgumentException ignored) {
            // Envelope may carry a non-UUID tenant marker; ignore.
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }
}
