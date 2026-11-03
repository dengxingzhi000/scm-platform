package com.scmcloud.common.tenant;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;
import java.util.UUID;

/**
 * Propagates TenantContextHolder and MDC context to @Async thread pools.
 * 
 * Captures tenant ID and all MDC entries (traceId, requestId, userId, username)
 * from the calling thread and restores them in the async thread, then cleans up
 * after execution completes.
 *
 * Usage: Configure on ThreadPoolTaskExecutor via setTaskDecorator().
 *
 * @author SCM Platform
 * @since 2026-06-04
 */
@Slf4j
public class TenantAwareMDCTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Capture context from calling thread
        UUID tenantId = TenantContextHolder.getTenantId();
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        return () -> {
            try {
                // Restore tenant context
                if (tenantId != null) {
                    TenantContextHolder.setTenantId(tenantId);
                }

                // Restore MDC context
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }

                runnable.run();
            } finally {
                // Always clean up to prevent thread-local leaks
                TenantContextHolder.clear();
                MDC.clear();
            }
        };
    }
}
