package com.scmcloud.common.tenant.metering;

import com.scmcloud.common.redis.script.RedisLuaScriptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Enforces tenant API quota using Redis Lua scripts for atomic check-and-increment.
 *
 * <p>Quota limits are cached in-memory (refreshed on service restart).
 * The Redis counter uses INCR + EXPIRE for daily reset.</p>
 *
 * <p>Lua script ensures atomicity: check limit → increment → set expiry in a single round-trip.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuotaEnforcer {
    private final StringRedisTemplate redisTemplate;

    private static final String COUNTER_PREFIX = "metering:";
    private static final long DEFAULT_DAILY_LIMIT = 100_000L;

    private static final RedisScript<Long> QUOTA_SCRIPT =
            RedisLuaScriptLoader.load("lua/tenant/check_and_increment_quota.lua", Long.class);

    // In-memory quota cache: tenantId -> daily limit
    private final ConcurrentMap<UUID, Long> quotaLimits = new ConcurrentHashMap<>();

    /**
     * Check if the tenant has quota remaining and increment the counter atomically.
     *
     * @return true if allowed, false if quota exceeded
     */
    public boolean checkAndIncrement(UUID tenantId) {
        String key = counterKey(tenantId);
        long limit = quotaLimits.getOrDefault(tenantId, DEFAULT_DAILY_LIMIT);
        long ttlSeconds = secondsUntilEndOfDay();

        Long result = redisTemplate.execute(QUOTA_SCRIPT, Collections.singletonList(key),
                String.valueOf(limit), String.valueOf(ttlSeconds));

        boolean allowed = result != null && result == 1L;
        if (!allowed) {
            log.warn("API quota exceeded for tenant {}: limit={}", tenantId, limit);
        }
        return allowed;
    }

    /**
     * Set the daily API quota for a tenant.
     */
    public void setQuota(UUID tenantId, long dailyLimit) {
        quotaLimits.put(tenantId, dailyLimit);
        log.info("Set API quota for tenant {}: {}", tenantId, dailyLimit);
    }

    /**
     * Get the current usage count for a tenant today.
     */
    public long getCurrentUsage(UUID tenantId) {
        String key = counterKey(tenantId);
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0 : Long.parseLong(value);
    }

    /**
     * Get the quota limit for a tenant.
     */
    public long getQuotaLimit(UUID tenantId) {
        return quotaLimits.getOrDefault(tenantId, DEFAULT_DAILY_LIMIT);
    }

    private String counterKey(UUID tenantId) {
        String date = LocalDate.now(ZoneOffset.UTC).toString();
        return COUNTER_PREFIX + tenantId + ":" + date;
    }

    private long secondsUntilEndOfDay() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        long nowEpoch = java.time.Instant.now().getEpochSecond();
        long endOfDayEpoch = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        return Math.max(1, endOfDayEpoch - nowEpoch);
    }
}
