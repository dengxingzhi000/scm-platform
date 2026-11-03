package com.scmcloud.analytics.ingest;

/**
 * Idempotency check for ODS ingestion.
 * <p>
 * Used to guard against duplicate processing of the same Kafka envelope
 * (e.g. broker redelivery after consumer failure).
 */
public interface OdsDedupService {

    /**
     * Atomically claim ownership of {@code eventId} for processing.
     *
     * @return {@code true} if this is the first time the event has been seen
     *         within the TTL window; {@code false} if it is a duplicate.
     */
    boolean tryAcquire(String eventId);
}
