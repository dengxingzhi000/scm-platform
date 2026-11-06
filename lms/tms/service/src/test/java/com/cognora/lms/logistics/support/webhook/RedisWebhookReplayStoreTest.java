package com.cognora.lms.logistics.support.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis nonce 存储用例：验证 SETNX + 2 倍窗口 TTL 的原子消费语义与降级路径。
 */
class RedisWebhookReplayStoreTest {

    private static final long NOW = 1_800_000_000L;
    private static final long WINDOW = 300;

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisWebhookReplayStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        store = new RedisWebhookReplayStore(redisTemplate);
    }

    @Test
    void firstConsumeShouldSucceedWithDoubleWindowTtl() {
        when(valueOperations.setIfAbsent(eq("lms:webhook:nonce:n-1"), eq("1"),
                eq(Duration.ofSeconds(WINDOW * 2)))).thenReturn(true);

        assertTrue(store.consumeIfAbsent("n-1", NOW, WINDOW));

        verify(valueOperations).setIfAbsent("lms:webhook:nonce:n-1", "1", Duration.ofSeconds(600));
    }

    @Test
    void repeatedConsumeShouldBeRejected() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true, false);

        assertTrue(store.consumeIfAbsent("n-2", NOW, WINDOW));
        assertFalse(store.consumeIfAbsent("n-2", NOW, WINDOW));
    }

    @Test
    void redisUnavailableShouldFallBackToLocalOnceOnly() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        assertTrue(store.consumeIfAbsent("n-3", NOW, WINDOW));
        // 降级为进程内语义后，同一实例内仍然拒绝重复 nonce。
        assertFalse(store.consumeIfAbsent("n-3", NOW, WINDOW));
    }
}
