package com.scmcloud.common.cache.metrics;

import com.scmcloud.common.redis.script.LuaScriptRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for Redis Lua scripts.
 *
 * <p>Reports script registration count, preload status, and Redis connectivity.</p>
 *
 * @author SCM Platform
 * @since 2026-06-29
 */
@Component
@RequiredArgsConstructor
public class LuaScriptHealthIndicator implements HealthIndicator {

    private final LuaScriptRegistry registry;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        try {
            String pong = redisTemplate.getRequiredConnectionFactory().getConnection().ping();
            boolean redisOk = pong != null && !pong.isEmpty();

            int total = registry.getScriptCount();
            int preloaded = registry.getPreloadedCount();
            int failed = registry.getPreloadFailedCount();

            Map<String, Object> details = new HashMap<>();
            details.put("redis", redisOk ? "UP" : "DOWN");
            details.put("scripts.registered", total);
            details.put("scripts.preloaded", preloaded);
            details.put("scripts.preloadFailed", failed);
            details.put("scripts.names", registry.getRegisteredNames());

            Health.Builder builder = redisOk ? Health.up() : Health.down();
            return builder.withDetails(details).build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
