package com.scmcloud.common.tenant.metering;

import com.scmcloud.common.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Interceptor that meters API calls per tenant per day.
 * Uses Redis INCR with daily expiry for atomic counters.
 *
 * <p>Counter key pattern: {@code metering:{tenantId}:{date}}
 * Expiry: end of day (UTC).</p>
 *
 * <p>Plug this into a Spring HandlerInterceptor registry to enable
 * real-time quota enforcement and usage-based billing data.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class MeteringInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final QuotaEnforcer quotaEnforcer;

    private static final String COUNTER_PREFIX = "metering:";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return true;
        }

        if (!quotaEnforcer.checkAndIncrement(tenantId)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":429,\"message\":\"API quota exceeded for today\"}");
            return false;
        }

        return true;
    }

    /**
     * Get the current API call count for a tenant today.
     */
    public long getCurrentCount(UUID tenantId) {
        String key = counterKey(tenantId);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Malformed API counter value for key={}, treating as zero", key);
            return 0;
        }
    }

    private String counterKey(UUID tenantId) {
        String date = LocalDate.now(ZoneOffset.UTC).toString();
        return COUNTER_PREFIX + tenantId + ":" + date;
    }
}
