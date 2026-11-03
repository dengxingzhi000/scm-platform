package com.cognora.platform.common.log.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cognora.platform.common.log.aspect.SysAuditLogAspect;
import com.cognora.platform.common.log.service.ISysAuditLogService;
import com.cognora.platform.common.log.service.LoggingSysAuditLogService;
import com.cognora.platform.common.log.service.RemoteSysAuditLogService;
import com.cognora.platform.common.rest.client.SysAuditServiceClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Wires the audit sink and the aspect that records {@code @AuditLog} methods.
 *
 * <p>Everything is registered here (not through component scanning) so it also applies to
 * services such as scm-auth that only scan their own package. scm-audit provides its own
 * persistent {@link ISysAuditLogService}; every other service reports over HTTP.
 */
@AutoConfiguration
public class AuditLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ISysAuditLogService.class)
    public ISysAuditLogService sysAuditLogService(ObjectProvider<SysAuditServiceClient> auditClientProvider,
                                                  ObjectProvider<MeterRegistry> meterRegistryProvider) {
        SysAuditServiceClient client = auditClientProvider.getIfAvailable();
        return client != null
                ? new RemoteSysAuditLogService(client, meterRegistryProvider.getIfAvailable())
                : new LoggingSysAuditLogService();
    }

    @Bean
    @ConditionalOnMissingBean(SysAuditLogAspect.class)
    public SysAuditLogAspect sysAuditLogAspect(ISysAuditLogService auditLogService, ObjectMapper objectMapper) {
        return new SysAuditLogAspect(auditLogService, objectMapper);
    }
}
