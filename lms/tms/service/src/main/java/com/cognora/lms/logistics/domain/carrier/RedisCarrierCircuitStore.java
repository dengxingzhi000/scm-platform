package com.cognora.lms.logistics.domain.carrier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis 支撑的熔断状态存储：多实例共享失败计数与打开状态。
 *
 * <p>键设计：{@code lms:carrier:circuit:{carrierId}:failures} 为连续失败计数，
 * {@code :open} 为熔断截止时间（毫秒），两者都带 TTL，冷却期结束自动过期，
 * 避免长期运行产生无界键。Redis 不可用时降级为进程内状态并告警。</p>
 */
@Slf4j
public class RedisCarrierCircuitStore implements CarrierCircuitStore {

    private static final String KEY_PREFIX = "lms:carrier:circuit:";
    private static final String SUFFIX_FAILURES = ":failures";
    private static final String SUFFIX_OPEN = ":open";

    private final StringRedisTemplate redisTemplate;
    private final CarrierCircuitStore fallback;

    public RedisCarrierCircuitStore(StringRedisTemplate redisTemplate) {
        this(redisTemplate, new InMemoryCarrierCircuitStore());
    }

    RedisCarrierCircuitStore(StringRedisTemplate redisTemplate, CarrierCircuitStore fallback) {
        this.redisTemplate = redisTemplate;
        this.fallback = fallback;
    }

    @Override
    public boolean isOpen(String carrierId, long nowMillis) {
        try {
            String openUntil = redisTemplate.opsForValue().get(openKey(carrierId));
            if (openUntil == null || openUntil.isBlank()) {
                return false;
            }
            return Long.parseLong(openUntil) > nowMillis;
        } catch (RuntimeException redisUnavailable) {
            log.warn("承运商熔断 Redis 读取失败，降级进程内状态: {}",
                    redisUnavailable.getClass().getSimpleName());
            return fallback.isOpen(carrierId, nowMillis);
        }
    }

    @Override
    public void recordFailure(String carrierId, int failureThreshold, long cooldownMillis, long nowMillis) {
        Duration keep = Duration.ofMillis(Math.max(cooldownMillis, 1_000L));
        try {
            Long failures = redisTemplate.opsForValue().increment(failureKey(carrierId));
            long count = failures == null ? 1L : failures;
            // 计数随冷却期滚动过期；冷却结束后重新累计。
            redisTemplate.expire(failureKey(carrierId), keep);
            if (count >= failureThreshold) {
                redisTemplate.opsForValue().set(openKey(carrierId),
                        String.valueOf(nowMillis + cooldownMillis), keep);
            }
        } catch (RuntimeException redisUnavailable) {
            log.warn("承运商熔断 Redis 写入失败，降级进程内状态: {}",
                    redisUnavailable.getClass().getSimpleName());
            fallback.recordFailure(carrierId, failureThreshold, cooldownMillis, nowMillis);
        }
    }

    @Override
    public void recordSuccess(String carrierId) {
        try {
            redisTemplate.delete(java.util.List.of(failureKey(carrierId), openKey(carrierId)));
        } catch (RuntimeException redisUnavailable) {
            log.warn("承运商熔断 Redis 清理失败，降级进程内状态: {}",
                    redisUnavailable.getClass().getSimpleName());
            fallback.recordSuccess(carrierId);
        }
    }

    private String failureKey(String carrierId) {
        return KEY_PREFIX + carrierId + SUFFIX_FAILURES;
    }

    private String openKey(String carrierId) {
        return KEY_PREFIX + carrierId + SUFFIX_OPEN;
    }
}
