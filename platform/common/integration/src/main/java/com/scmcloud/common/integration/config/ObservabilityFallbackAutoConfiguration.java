package com.scmcloud.common.integration.config;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * The integration publishers/consumers always take a {@link Tracer}. Without an OpenTelemetry
 * SDK on the classpath no auto-configuration provides one, so every service using
 * scm-common-integration failed to start; a no-op tracer keeps them running untraced.
 */
@AutoConfiguration
@ConditionalOnClass(TracerProvider.class)
public class ObservabilityFallbackAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Tracer.class)
    public Tracer noopTracer() {
        return TracerProvider.noop().get("scm-common-integration");
    }
}
