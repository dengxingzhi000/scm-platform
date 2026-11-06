package com.cognora.lms.logistics.domain.carrier;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 跨实例熔断语义：两个执行器共享同一熔断存储时，一个实例打开的熔断对另一个实例生效。
 */
class CarrierCircuitCrossInstanceTest {

    @Test
    void circuitOpenedOnOneInstanceShouldSkipCarrierOnAnother() {
        CarrierCircuitStore sharedStore = new InMemoryCarrierCircuitStore();
        CarrierFailoverExecutor nodeA = new CarrierFailoverExecutor(
                2, 60_000L, 2, 100L, 1_000L, millis -> { }, sharedStore);
        CarrierFailoverExecutor nodeB = new CarrierFailoverExecutor(
                2, 60_000L, 2, 100L, 1_000L, millis -> { }, sharedStore);

        AtomicInteger primaryCalls = new AtomicInteger();
        // 实例 A 连续失败两次，熔断打开。
        nodeA.execute("A", List.of(), carrier -> {
            primaryCalls.incrementAndGet();
            throw new IllegalStateException("carrier A down");
        });
        assertEquals(2, primaryCalls.get());

        // 实例 B 读取共享状态，直接跳过 A 并降级到 B。
        CarrierFailoverExecutor.CallOutcome<String> outcome = nodeB.execute("A", List.of("B"), carrier -> {
            if ("A".equals(carrier)) {
                primaryCalls.incrementAndGet();
                throw new IllegalStateException("carrier A down");
            }
            return "ok";
        });

        assertTrue(outcome.success());
        assertEquals("B", outcome.carrierId());
        assertEquals(1, outcome.attempts());
        assertTrue(outcome.degraded());
        assertEquals(2, primaryCalls.get(), "实例 B 不应再调用已熔断的 A");
    }
}
