package com.scmcloud.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TenantAwareMDCTaskDecoratorTest {

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
        MDC.clear();
    }

    @Test
    void shouldPropagateTenantIdToAsyncThread() throws Exception {
        // Given
        UUID expectedTenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(expectedTenantId);

        ThreadPoolTaskExecutor executor = createExecutor();
        executor.setTaskDecorator(new TenantAwareMDCTaskDecorator());
        executor.initialize();

        // When
        CompletableFuture<UUID> future = CompletableFuture.supplyAsync(
                TenantContextHolder::getTenantId, executor);

        // Then
        UUID actualTenantId = future.get(5, TimeUnit.SECONDS);
        assertEquals(expectedTenantId, actualTenantId);

        executor.shutdown();
    }

    @Test
    void shouldPropagateMDCContextToAsyncThread() throws Exception {
        // Given
        MDC.put("traceId", "test-trace-123");
        MDC.put("requestId", "req-456");
        MDC.put("userId", "user-789");

        ThreadPoolTaskExecutor executor = createExecutor();
        executor.setTaskDecorator(new TenantAwareMDCTaskDecorator());
        executor.initialize();

        // When
        CompletableFuture<String> future = CompletableFuture.supplyAsync(
                () -> MDC.get("traceId"), executor);

        // Then
        String traceId = future.get(5, TimeUnit.SECONDS);
        assertEquals("test-trace-123", traceId);

        executor.shutdown();
    }

    @Test
    void shouldPropagateBothTenantAndMDC() throws Exception {
        // Given
        UUID expectedTenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(expectedTenantId);
        MDC.put("traceId", "trace-abc");
        MDC.put("requestId", "req-xyz");

        ThreadPoolTaskExecutor executor = createExecutor();
        executor.setTaskDecorator(new TenantAwareMDCTaskDecorator());
        executor.initialize();

        // When
        CompletableFuture<UUID> tenantFuture = CompletableFuture.supplyAsync(
                TenantContextHolder::getTenantId, executor);
        CompletableFuture<String> traceFuture = CompletableFuture.supplyAsync(
                () -> MDC.get("traceId"), executor);
        CompletableFuture<String> requestFuture = CompletableFuture.supplyAsync(
                () -> MDC.get("requestId"), executor);

        // Then
        assertEquals(expectedTenantId, tenantFuture.get(5, TimeUnit.SECONDS));
        assertEquals("trace-abc", traceFuture.get(5, TimeUnit.SECONDS));
        assertEquals("req-xyz", requestFuture.get(5, TimeUnit.SECONDS));

        executor.shutdown();
    }

    @Test
    void shouldClearContextAfterExecution() throws Exception {
        // Given
        UUID expectedTenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(expectedTenantId);
        MDC.put("traceId", "trace-to-clear");

        ThreadPoolTaskExecutor executor = createExecutor();
        executor.setTaskDecorator(new TenantAwareMDCTaskDecorator());
        executor.initialize();

        // When - run a task and verify context is propagated during execution
        AtomicReference<UUID> capturedTenantId = new AtomicReference<>();
        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            capturedTenantId.set(TenantContextHolder.getTenantId());
            capturedTraceId.set(MDC.get("traceId"));
        }, executor);

        future.get(5, TimeUnit.SECONDS);
        assertEquals(expectedTenantId, capturedTenantId.get());
        assertEquals("trace-to-clear", capturedTraceId.get());

        // Clear main thread context so decorator won't re-propagate
        TenantContextHolder.clear();
        MDC.clear();

        // Then - submit another task on the same pooled thread;
        // context should be cleared by the first task's finally block
        CompletableFuture<UUID> future2 = CompletableFuture.supplyAsync(
                TenantContextHolder::getTenantId, executor);
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(
                () -> MDC.get("traceId"), executor);

        assertNull(future2.get(5, TimeUnit.SECONDS));
        assertNull(future3.get(5, TimeUnit.SECONDS));

        executor.shutdown();
    }

    @Test
    void shouldHandleNullContextGracefully() throws Exception {
        // Given - no context set
        TenantContextHolder.clear();
        MDC.clear();

        ThreadPoolTaskExecutor executor = createExecutor();
        executor.setTaskDecorator(new TenantAwareMDCTaskDecorator());
        executor.initialize();

        // When
        CompletableFuture<UUID> tenantFuture = CompletableFuture.supplyAsync(
                TenantContextHolder::getTenantId, executor);
        CompletableFuture<String> traceFuture = CompletableFuture.supplyAsync(
                () -> MDC.get("traceId"), executor);

        // Then
        assertNull(tenantFuture.get(5, TimeUnit.SECONDS));
        assertNull(traceFuture.get(5, TimeUnit.SECONDS));

        executor.shutdown();
    }

    private ThreadPoolTaskExecutor createExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(5);
        executor.setThreadNamePrefix("test-async-");
        return executor;
    }
}
