package com.cognora.platform.common.tenant;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Auto-configures @Async thread pools with tenant context propagation.
 * Ensures TenantContextHolder is available in async threads.
 */
@AutoConfiguration
@EnableAsync
@EnableConfigurationProperties(TenantProperties.class)
public class AsyncTenantAutoConfiguration {

    @Bean(name = "tenantAwareTaskExecutor")
    public Executor tenantAwareTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-tenant-");
        executor.setTaskDecorator(new TenantAwareMDCTaskDecorator());
        executor.initialize();
        return executor;
    }
}
