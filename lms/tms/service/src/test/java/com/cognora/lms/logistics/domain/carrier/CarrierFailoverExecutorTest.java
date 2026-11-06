package com.cognora.lms.logistics.domain.carrier;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 批次 3：承运商重试退避、熔断与降级用例。
 */
class CarrierFailoverExecutorTest {

    @Test
    void retryShouldUseExponentialBackoffAndSucceedOnPrimary() {
        List<Long> sleeps = new ArrayList<>();
        CarrierFailoverExecutor executor = new CarrierFailoverExecutor(
                3, 60_000L, 3, 100L, 1_000L, sleeps::add);
        AtomicInteger calls = new AtomicInteger();

        CarrierFailoverExecutor.CallOutcome<String> outcome = executor.execute("A", List.of("B"), carrier -> {
            if (calls.incrementAndGet() < 3) {
                throw new IllegalStateException("timeout");
            }
            return "ok";
        });

        assertTrue(outcome.success());
        assertEquals("A", outcome.carrierId());
        assertEquals(3, outcome.attempts());
        assertFalse(outcome.degraded());
        // 退避为 base×2^n：第 1 次失败后 100ms，第 2 次失败后 200ms。
        assertEquals(List.of(100L, 200L), sleeps);
    }

    @Test
    void primaryFailureShouldDegradeToBackupCarrier() {
        CarrierFailoverExecutor executor = new CarrierFailoverExecutor(
                3, 60_000L, 2, 100L, 1_000L, millis -> { });
        List<String> attempted = new ArrayList<>();

        CarrierFailoverExecutor.CallOutcome<String> outcome = executor.execute("A", List.of("B"), carrier -> {
            attempted.add(carrier);
            if ("A".equals(carrier)) {
                throw new IllegalStateException("carrier A unavailable");
            }
            return "ok";
        });

        assertTrue(outcome.success());
        assertEquals("B", outcome.carrierId());
        assertEquals(3, outcome.attempts());
        assertTrue(outcome.degraded());
        assertEquals(List.of("A", "A", "B"), attempted);
    }

    @Test
    void circuitShouldOpenAfterThresholdAndSkipPrimaryOnNextCall() {
        CarrierFailoverExecutor executor = new CarrierFailoverExecutor(
                2, 60_000L, 2, 100L, 1_000L, millis -> { });
        AtomicInteger primaryCalls = new AtomicInteger();

        CarrierFailoverExecutor.CallOutcome<String> first = executor.execute("A", List.of(), carrier -> {
            primaryCalls.incrementAndGet();
            throw new IllegalStateException("carrier A down");
        });

        assertFalse(first.success());
        assertTrue(first.degraded());
        assertEquals(2, primaryCalls.get());

        CarrierFailoverExecutor.CallOutcome<String> second = executor.execute("A", List.of("B"), carrier -> {
            if ("A".equals(carrier)) {
                primaryCalls.incrementAndGet();
                throw new IllegalStateException("carrier A down");
            }
            return "ok";
        });

        assertTrue(second.success());
        assertEquals("B", second.carrierId());
        assertEquals(1, second.attempts());
        assertTrue(second.degraded());
        assertEquals(2, primaryCalls.get(), "熔断期内不应再调用主承运商");
    }

    @Test
    void allCircuitsOpenShouldFailWithoutCallingCarrier() {
        CarrierFailoverExecutor executor = new CarrierFailoverExecutor(
                1, 60_000L, 1, 100L, 1_000L, millis -> { });
        Function<String, String> alwaysFail = carrier -> {
            throw new IllegalStateException(carrier + " down");
        };

        CarrierFailoverExecutor.CallOutcome<String> opened = executor.execute("A", List.of("B"), alwaysFail);
        assertFalse(opened.success());
        assertEquals(2, opened.attempts());
        assertEquals("B down", opened.error());

        CarrierFailoverExecutor.CallOutcome<String> skipped = executor.execute("A", List.of("B"), alwaysFail);
        assertFalse(skipped.success());
        assertEquals(0, skipped.attempts());
        assertTrue(skipped.degraded());
    }

    @Test
    void allCarriersFailingShouldReturnLastError() {
        CarrierFailoverExecutor executor = new CarrierFailoverExecutor(
                10, 60_000L, 2, 100L, 1_000L, millis -> { });

        CarrierFailoverExecutor.CallOutcome<String> outcome = executor.execute("A", List.of("B"), carrier -> {
            throw new IllegalStateException(carrier + " timeout");
        });

        assertFalse(outcome.success());
        assertNull(outcome.carrierId());
        assertEquals(4, outcome.attempts());
        assertEquals("B timeout", outcome.error());
    }

    @Test
    void successShouldResetFailureCount() {
        CarrierFailoverExecutor executor = new CarrierFailoverExecutor(
                3, 60_000L, 1, 100L, 1_000L, millis -> { });
        AtomicInteger calls = new AtomicInteger();

        // 两次失败（未达阈值），一次成功后计数清零，再两次失败仍不应熔断。
        for (int i = 0; i < 2; i++) {
            executor.execute("A", List.of(), carrier -> {
                calls.incrementAndGet();
                throw new IllegalStateException("flaky");
            });
        }
        executor.execute("A", List.of(), carrier -> {
            calls.incrementAndGet();
            return "ok";
        });
        for (int i = 0; i < 2; i++) {
            executor.execute("A", List.of(), carrier -> {
                calls.incrementAndGet();
                throw new IllegalStateException("flaky");
            });
        }

        CarrierFailoverExecutor.CallOutcome<String> outcome = executor.execute("A", List.of(), carrier -> {
            calls.incrementAndGet();
            return "ok";
        });

        assertTrue(outcome.success());
        assertEquals(6, calls.get(), "熔断应未打开，第五次调用仍走真实主承运商");
    }
}
