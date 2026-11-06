package com.cognora.lms.logistics.support.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis 支撑的 nonce 防重放存储：多实例共享，SETNX + TTL 原子消费。
 *
 * <p>TTL 取 2 倍时间窗：请求时间戳只要仍在窗口内，其 nonce 就必须继续保持可见，
 * 否则跨窗口边界的重放可能漏过。Redis 不可用时回退进程内存储并告警，
 * 最终仍有 tms_tracking 的 {@code (tenant_id, event_id)} 唯一约束兜底。</p>
 */
@Slf4j
public class RedisWebhookReplayStore implements WebhookReplayStore {

    private static final String KEY_PREFIX = "lms:webhook:nonce:";

    private final StringRedisTemplate redisTemplate;
    private final WebhookReplayStore fallback;

    public RedisWebhookReplayStore(StringRedisTemplate redisTemplate) {
        this(redisTemplate, new InMemoryWebhookReplayStore());
    }

    RedisWebhookReplayStore(StringRedisTemplate redisTemplate, WebhookReplayStore fallback) {
        this.redisTemplate = redisTemplate;
        this.fallback = fallback;
    }

    @Override
    public boolean consumeIfAbsent(String nonce, long nowSeconds, long windowSeconds) {
        String key = KEY_PREFIX + nonce;
        long ttlSeconds = Math.max(windowSeconds * 2, 1L);
        try {
            Boolean fresh = redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds));
            return Boolean.TRUE.equals(fresh);
        } catch (RuntimeException redisUnavailable) {
            // 不打印 key/nonce 之外的信息；降级为单实例语义并明确告警。
            log.warn("Webhook nonce Redis 不可用，降级进程内防重放: {}", redisUnavailable.getClass().getSimpleName());
            return fallback.consumeIfAbsent(nonce, nowSeconds, windowSeconds);
        }
    }
}
