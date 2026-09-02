package com.scmcloud.analytics.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.integration.outbox.OutboxEvent;
import com.scmcloud.common.tenant.TenantContextHolder;
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
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Subscribes to business CDC topics published by the outbox poller
 * ({@code scm.ordorder}, {@code scm.inventory}) and feeds transformed rows
 * into a thread-safe bounded queue drained by {@link #flushPending()}.
 *
 * <p>Design notes:
 * <ul>
 *   <li>Batch listener — one call delivers {@code List<ConsumerRecord>}; we
 *       ack the whole batch at the end so a partial parse error can fail the
 *       batch and trigger redelivery.</li>
 *   <li>Dedup uses the inner {@link OutboxEvent#getId()} — NOT the envelope
 *       id, which is regenerated every time {@code OutboxPoller} republishes
 *       (retry / redelivery). Falling back to {@code envelope.id} only when
 *       the inner payload cannot be parsed as an OutboxEvent.</li>
 *   <li>The pending queue is bounded ({@link #DEFAULT_QUEUE_CAPACITY}). When
 *       full, the listener applies backpressure to Kafka by NOT acking the
 *       batch — records will be redelivered after consumer restart. This
 *       prevents heap blow-up under burst.</li>
 *   <li>If {@link #flushPending()} fails repeatedly ({@link #MAX_CONSECUTIVE_FLUSH_FAILURES}
 *       consecutive times), the consumer opens a circuit break: new records
 *       are NOT enqueued AND NOT acked, so they are redelivered after a
 *       restart once ClickHouse recovers.</li>
 *   <li>On flush failure, the drained batch is re-enqueued at the FRONT of
 *       the queue (order preserved) so the next tick retries those rows
 *       before any newly-arriving rows.</li>
 * </ul>
 */
@Slf4j
@Component
public class OdsIngestConsumer {

    public static final int FLUSH_BATCH_SIZE = 1000;
    public static final int DEFAULT_QUEUE_CAPACITY = 10_000;
    public static final int MAX_CONSECUTIVE_FLUSH_FAILURES = 5;
    public static final long OFFER_TIMEOUT_SECONDS = 5L;
    public static final String TOPIC_ORDER = "scm.ordorder";
    public static final String TOPIC_INVENTORY = "scm.inventory";

    private final ClickHouseOdsWriter writer;
    private final OdsDedupService dedupService;
    private final ObjectMapper objectMapper;
    private final BlockingDeque<OdsRow> pendingRows;

    private final AtomicInteger consecutiveFlushFailures = new AtomicInteger(0);
    private final AtomicLong droppedBatchCounter = new AtomicLong(0);

    public OdsIngestConsumer(ClickHouseOdsWriter writer,
                             OdsDedupService dedupService,
                             ObjectMapper objectMapper) {
        this(writer, dedupService, objectMapper, DEFAULT_QUEUE_CAPACITY);
    }

    public OdsIngestConsumer(ClickHouseOdsWriter writer,
                             OdsDedupService dedupService,
                             ObjectMapper objectMapper,
                             int queueCapacity) {
        this.writer = writer;
        this.dedupService = dedupService;
        this.objectMapper = objectMapper;
        this.pendingRows = new LinkedBlockingDeque<>(queueCapacity);
    }

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

        boolean allEnqueued = true;
        for (ConsumerRecord<String, String> record : records) {
            if (!handleSingle(record)) {
                allEnqueued = false;
            }
        }
        if (allEnqueued) {
            ack.acknowledge();
        } else {
            log.warn("Backpressure / circuit break — NOT acking batch of {} records; they will be redelivered after consumer restart",
                    records.size());
        }
    }

    /**
     * Process a single Kafka record.
     *
     * @return {@code true} if the record was successfully enqueued (or
     *         intentionally skipped as a duplicate); {@code false} if the
     *         queue is full or the circuit is broken — caller should NOT ack.
     * @throws RuntimeException on parse / processing failure; caller should
     *         let this propagate so Spring Kafka redelivers the batch.
     */
    private boolean handleSingle(ConsumerRecord<String, String> record) {
        try {
            String envelopeJson = record.value();
            if (envelopeJson == null || envelopeJson.isEmpty()) {
                log.debug("Skipping empty record at offset {}", record.offset());
                return true;
            }

            JsonNode envelope = objectMapper.readTree(envelopeJson);
            String envelopeId = textOrNull(envelope, "id");

            ParsedInner parsed = parseInner(envelope);
            // Dedup key: prefer the stable inner event id; fall back to envelope id.
            String dedupKey = parsed.eventId != null ? parsed.eventId : envelopeId;
            if (dedupKey == null || dedupKey.isBlank()) {
                log.warn("Skipping envelope with no dedup key at offset {}", record.offset());
                return true;
            }
            if (!dedupService.tryAcquire(dedupKey)) {
                return true;
            }

            String tenantId = textOrNull(envelope, "tenantId");
            setTenantContext(tenantId);

            JsonNode innerPayload = parsed.payload;
            if (innerPayload == null || innerPayload.isMissingNode()) {
                log.warn("Skipping envelope with no inner payload: id={}", envelopeId);
                return true;
            }

            OdsRow row = switch (record.topic()) {
                case TOPIC_ORDER -> OdsRowMapper.orderFromPayload(
                        record.topic(), innerPayload, record.offset(), record.timestamp());
                case TOPIC_INVENTORY -> OdsRowMapper.inventoryFromPayload(
                        record.topic(), innerPayload, record.offset(), record.timestamp());
                default -> {
                    log.debug("Ignoring topic {} for envelope {}", record.topic(), envelopeId);
                    yield null;
                }
            };
            if (row != null) {
                if (isCircuitBroken()) {
                    log.error("Circuit broken (consecutiveFlushFailures={}) — refusing to enqueue record at offset {}; "
                                    + "record will be redelivered after consumer restart",
                            consecutiveFlushFailures.get(), record.offset());
                    return false;
                }
                boolean offered;
                try {
                    offered = pendingRows.offerLast(row, OFFER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                if (!offered) {
                    log.error("Pending queue full after {}s — applying backpressure: record at offset {} will NOT be acked",
                            OFFER_TIMEOUT_SECONDS, record.offset());
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            // Parse / processing failure — let Spring Kafka redeliver the batch
            log.warn("Failed to process record at offset {}: {}", record.offset(), ex.getMessage());
            throw new RuntimeException(ex);
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * Parse the envelope's {@code data} field, attempting to interpret it as
     * an {@link OutboxEvent} JSON. Returns a stable inner event id (for dedup)
     * and the inner business payload (the value of {@code OutboxEvent.payload}
     * parsed as JSON) when available. Falls back to treating {@code data} as
     * the inner payload directly when it cannot be parsed as an OutboxEvent.
     */
    private ParsedInner parseInner(JsonNode envelope) {
        JsonNode dataNode = envelope.get("data");
        if (dataNode == null || dataNode.isNull()) {
            return new ParsedInner(null, null);
        }
        JsonNode inner = dataNode.isTextual() ? safeReadTree(dataNode.asText()) : dataNode;
        if (inner == null || !inner.isObject()) {
            return new ParsedInner(null, null);
        }
        String eventId = null;
        JsonNode payload = null;
        try {
            OutboxEvent event = objectMapper.treeToValue(inner, OutboxEvent.class);
            if (event != null && event.getId() != null && !event.getId().isBlank()) {
                eventId = event.getId();
            }
            if (event != null && event.getPayload() != null && !event.getPayload().isBlank()) {
                payload = safeReadTree(event.getPayload());
            }
        } catch (Exception ex) {
            log.debug("Could not parse envelope data as OutboxEvent (falling back to raw data): {}",
                    ex.getMessage());
        }
        // Fallback: when the inner is not an OutboxEvent (legacy / test path),
        // treat it as the business payload directly.
        if (payload == null) {
            payload = inner;
        }
        return new ParsedInner(eventId, payload);
    }

    private JsonNode safeReadTree(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Periodic flush — runs independently of Kafka cadence (every 5s by default,
     * overridable via {@code analytics.ods.flush-interval-ms} when added to the
     * configuration). Drains up to {@link #FLUSH_BATCH_SIZE} rows, groups by
     * table, and bulk-inserts each group.
     *
     * <p>On insert failure: the entire drained batch is re-enqueued at the
     * front of the queue (original order preserved) so the next scheduled
     * tick retries those rows before any newly-arriving ones. After
     * {@link #MAX_CONSECUTIVE_FLUSH_FAILURES} consecutive failures the
     * circuit is considered broken.</p>
     */
    @Scheduled(fixedDelayString = "${analytics.ods.flush-interval-ms:5000}")
    public void flushPending() {
        if (pendingRows.isEmpty()) {
            return;
        }
        log.debug("flushPending() draining up to {} rows (queue size {})",
                FLUSH_BATCH_SIZE, pendingRows.size());

        List<OdsRow> drained = new ArrayList<>(FLUSH_BATCH_SIZE);
        for (int i = 0; i < FLUSH_BATCH_SIZE; i++) {
            OdsRow row = pendingRows.pollFirst();
            if (row == null) {
                break;
            }
            drained.add(row);
        }
        if (drained.isEmpty()) {
            return;
        }

        Map<String, List<Map<String, Object>>> byTable = new HashMap<>();
        for (OdsRow row : drained) {
            byTable.computeIfAbsent(row.getTable(), t -> new ArrayList<>()).add(row.getFields());
        }
        for (Map.Entry<String, List<Map<String, Object>>> entry : byTable.entrySet()) {
            String table = entry.getKey();
            List<Map<String, Object>> rows = entry.getValue();
            try {
                writer.insertBatch(table, rows);
                consecutiveFlushFailures.set(0);
            } catch (Exception ex) {
                int failures = consecutiveFlushFailures.incrementAndGet();
                droppedBatchCounter.incrementAndGet();
                log.error("Flush failed (failure #{}/{}) for {} rows into {}: {}",
                        failures, MAX_CONSECUTIVE_FLUSH_FAILURES, rows.size(), table, ex.getMessage(), ex);
                // Re-enqueue the drained batch at the front of the queue in
                // original order, so the next tick retries before new rows.
                // Iterate in reverse so offerFirst produces FIFO order.
                for (int i = drained.size() - 1; i >= 0; i--) {
                    pendingRows.offerFirst(drained.get(i));
                }
                return; // break out of the flush loop
            }
        }
    }

    boolean isCircuitBroken() {
        return consecutiveFlushFailures.get() >= MAX_CONSECUTIVE_FLUSH_FAILURES;
    }

    int getConsecutiveFlushFailures() {
        return consecutiveFlushFailures.get();
    }

    long getDroppedBatchCounter() {
        return droppedBatchCounter.get();
    }

    /** Visible for tests — number of rows currently buffered. */
    int pendingCount() {
        return pendingRows.size();
    }

    /** Visible for tests — manual drain trigger that bypasses the schedule. */
    void flushNowForTest() {
        flushPending();
    }

    /** Visible for tests — reset the flush-failure circuit and dropped-batch
     * counter so they do not leak state across test cases that share a
     * Spring context. */
    void resetCircuitForTest() {
        consecutiveFlushFailures.set(0);
        droppedBatchCounter.set(0);
    }

    /**
     * Visible for tests — non-blocking offer. Used by the bounded-queue test
     * to verify the queue is bounded without waiting for the production
     * timed-offer timeout.
     */
    boolean offerNowForTest(OdsRow row) {
        return pendingRows.offerLast(row);
    }

    private record ParsedInner(String eventId, JsonNode payload) {
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