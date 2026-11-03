package com.scmcloud.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TenantAwareTaskDecoratorTest {

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldPropagateTenantIdToAsyncThread() throws Exception {
        // Given
        UUID expectedTenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(expectedTenantId);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setTaskDecorator(new TenantAwareTaskDecorator());
        executor.initialize();

        // When
        CompletableFuture<UUID> future = CompletableFuture.supplyAsync(() -> {
            return TenantContextHolder.getTenantId();
        }, executor);

        // Then
        UUID actualTenantId = future.get(5, TimeUnit.SECONDS);
        assertEquals(expectedTenantId, actualTenantId);

        executor.shutdown();
    }

    @Test
    void shouldClearTenantIdAfterExecution() throws Exception {
        // Given
        UUID expectedTenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(expectedTenantId);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setTaskDecorator(new TenantAwareTaskDecorator());
        executor.initialize();

        // When - run a task and verify tenantId is propagated during execution
        AtomicReference<UUID> capturedDuringExecution = new AtomicReference<>();
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            capturedDuringExecution.set(TenantContextHolder.getTenantId());
        }, executor);

        future.get(5, TimeUnit.SECONDS);
        assertEquals(expectedTenantId, capturedDuringExecution.get());

        // Clear main thread context so decorator won't re-propagate
        TenantContextHolder.clear();

        // Then - submit another task on the same pooled thread;
        // decorator captures null from main, and the async thread's
        // context was cleared by the first task's finally block
        CompletableFuture<UUID> future2 = CompletableFuture.supplyAsync(() -> {
            return TenantContextHolder.getTenantId();
        }, executor);

        UUID tenantIdAfterExecution = future2.get(5, TimeUnit.SECONDS);
        assertNull(tenantIdAfterExecution);

        executor.shutdown();
    }

    @Test
    void shouldHandleNullTenantIdGracefully() throws Exception {
        // Given - no tenant ID set
        TenantContextHolder.clear();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setTaskDecorator(new TenantAwareTaskDecorator());
        executor.initialize();

        // When
        CompletableFuture<UUID> future = CompletableFuture.supplyAsync(() -> {
            return TenantContextHolder.getTenantId();
        }, executor);

        // Then
        UUID actualTenantId = future.get(5, TimeUnit.SECONDS);
        assertNull(actualTenantId);

        executor.shutdown();
    }
}
