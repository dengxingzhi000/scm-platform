package com.scmcloud.analytics.ingest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed {@link OdsDedupService}.
 * <p>
 * Uses {@code SETNX} with a 24-hour TTL keyed on the Kafka envelope id.
 * Chosen over the {@code @Idempotent} annotation because the consumer needs
 * <em>silent</em> duplicate handling (just ack the record), whereas the
 * annotation throws — also the annotation forces a separate method that
 * breaks the natural flow inside a batch consumer loop.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(StringRedisTemplate.class)
public class RedisOdsDedupService implements OdsDedupService {

    public static final String KEY_PREFIX = "ods:dedup:";
    public static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryAcquire(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            log.warn("Skipping dedup for blank event id");
            return false;
        }
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + eventId, "1", TTL);
        boolean isNew = Boolean.TRUE.equals(acquired);
        if (!isNew) {
            log.debug("Duplicate ODS event skipped: {}", eventId);
        }
        return isNew;
    }
}
