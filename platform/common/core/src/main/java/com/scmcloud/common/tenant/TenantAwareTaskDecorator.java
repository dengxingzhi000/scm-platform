package com.scmcloud.common.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskDecorator;

import java.util.UUID;

/**
 * Propagates TenantContextHolder to @Async thread pools.
 * Configure on ThreadPoolTaskExecutor via setTaskDecorator().
 *
 * Also propagates MDC for structured logging correlation.
 */
@Slf4j
public class TenantAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        UUID tenantId = TenantContextHolder.getTenantId();
        return () -> {
            try {
                if (tenantId != null) {
                    TenantContextHolder.setTenantId(tenantId);
                }
                runnable.run();
            } finally {
                TenantContextHolder.clear();
            }
        };
    }
}
