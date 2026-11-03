package com.scmcloud.common.cache;

import com.scmcloud.common.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Cache key generator that automatically prepends tenantId to all cache keys.
 * Ensures cache isolation between tenants.
 *
 * <p>Usage with @Cacheable:</p>
 * <pre>
 * @Cacheable(value = "users", keyGenerator = "tenantAwareCacheKeyGenerator")
 * public User findById(UUID userId) { ... }
 * </pre>
 */
@Slf4j
public class TenantAwareCacheKeyGenerator implements KeyGenerator {

    @NonNull
    @Override
    public Object generate(@NonNull Object target, @NonNull Method method, @NonNull Object... params) {
        UUID tenantId = TenantContextHolder.getTenantId();
        String baseKey = SimpleKeyGenerator.generateKey(params).toString();

        if (tenantId != null) {
            return tenantId + ":" + baseKey;
        }

        log.warn("Tenant ID is null when generating cache key for {}.{}",
                target.getClass().getSimpleName(), method.getName());
        return "no-tenant:" + baseKey;
    }
}
