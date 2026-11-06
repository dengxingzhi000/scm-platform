package com.cognora.lms.logistics.domain.carrier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 熔断存储用例：验证 INCR + EXPIRE 累计、达阈值写 OPEN 截止时间、成功清键与降级路径。
 */
class RedisCarrierCircuitStoreTest {

    private static final long NOW = 1_800_000_000_000L;
    private static final long COOLDOWN = 60_000L;
    private static final String FAIL_KEY = "lms:carrier:circuit:A:failures";
    private static final String OPEN_KEY = "lms:carrier:circuit:A:open";

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisCarrierCircuitStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        store = new RedisCarrierCircuitStore(redisTemplate);
    }

    @Test
    void failuresBelowThresholdShouldNotOpenCircuit() {
        when(valueOperations.increment(FAIL_KEY)).thenReturn(1L, 2L);
        when(redisTemplate.expire(eq(FAIL_KEY), any(Duration.class))).thenReturn(true);

        store.recordFailure("A", 3, COOLDOWN, NOW);
        store.recordFailure("A", 3, COOLDOWN, NOW);

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        verify(redisTemplate, org.mockito.Mockito.times(2))
                .expire(FAIL_KEY, Duration.ofMillis(COOLDOWN));
    }

    @Test
    void thresholdShouldOpenWithCooldownDeadline() {
        when(valueOperations.increment(FAIL_KEY)).thenReturn(3L);
        when(redisTemplate.expire(eq(FAIL_KEY), any(Duration.class))).thenReturn(true);

        store.recordFailure("A", 3, COOLDOWN, NOW);

        verify(valueOperations).set(OPEN_KEY, String.valueOf(NOW + COOLDOWN), Duration.ofMillis(COOLDOWN));
    }

    @Test
    void openDeadlineInFutureShouldSkipCarrier() {
        when(valueOperations.get(OPEN_KEY)).thenReturn(String.valueOf(NOW + COOLDOWN));
        assertTrue(store.isOpen("A", NOW));

        when(valueOperations.get(OPEN_KEY)).thenReturn(String.valueOf(NOW - 1));
        assertFalse(store.isOpen("A", NOW));
    }

    @Test
    void successShouldClearFailureAndOpenKeys() {
        store.recordSuccess("A");
        verify(redisTemplate).delete(List.of(FAIL_KEY, OPEN_KEY));
    }

    @Test
    void redisUnavailableShouldDegradeToLocalSemantics() {
        when(valueOperations.increment(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        store.recordFailure("A", 1, COOLDOWN, NOW);
        assertTrue(store.isOpen("A", NOW), "应回退到进程内状态并保持熔断打开");
    }
}
