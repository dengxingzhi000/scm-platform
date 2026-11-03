package com.scmcloud.common.cache;

import com.scmcloud.common.cache.spring.TwoLevelCache;
import com.scmcloud.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TwoLevelCacheTenantIsolationTest {

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldIsolateCacheBetweenTenants() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        ConcurrentHashMap<String, Object> redisStorage = new ConcurrentHashMap<>();
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            redisStorage.put(key, value);
            return null;
        }).when(valueOps).set(anyString(), any(), any(Duration.class));

        when(valueOps.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return redisStorage.get(key);
        });

        TwoLevelCache cache = new TwoLevelCache("test-cache", Duration.ofMinutes(5), redisTemplate, 100);

        TenantContextHolder.setTenantId(tenantA);
        cache.put("user:123", "Alice");

        TenantContextHolder.setTenantId(tenantB);
        Object valueForTenantB = cache.get("user:123", String.class);
        assertNull(valueForTenantB);

        TenantContextHolder.setTenantId(tenantA);
        Object valueForTenantA = cache.get("user:123", String.class);
        assertEquals("Alice", valueForTenantA);
    }

    @Test
    void shouldGenerateDifferentKeysForDifferentTenants() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        ConcurrentHashMap<String, Object> redisStorage = new ConcurrentHashMap<>();
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            redisStorage.put(key, value);
            return null;
        }).when(valueOps).set(anyString(), any(), any(Duration.class));

        when(valueOps.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return redisStorage.get(key);
        });

        TwoLevelCache cache = new TwoLevelCache("test-cache", Duration.ofMinutes(5), redisTemplate, 100);

        TenantContextHolder.setTenantId(tenantA);
        cache.put("shared-key", "value-from-tenant-a");

        TenantContextHolder.setTenantId(tenantB);
        cache.put("shared-key", "value-from-tenant-b");

        TenantContextHolder.setTenantId(tenantA);
        assertEquals("value-from-tenant-a", cache.get("shared-key", String.class));

        TenantContextHolder.setTenantId(tenantB);
        assertEquals("value-from-tenant-b", cache.get("shared-key", String.class));
    }
}
