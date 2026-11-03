package com.scmcloud.common.log.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.log.aspect.SysAuditLogAspect;
import com.scmcloud.common.log.service.ISysAuditLogService;
import com.scmcloud.common.log.service.LoggingSysAuditLogService;
import com.scmcloud.common.log.service.RemoteSysAuditLogService;
import com.scmcloud.common.rest.client.SysAuditServiceClient;
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
    public ISysAuditLogService sysAuditLogService(ObjectProvider<SysAuditServiceClient> auditClientProvider) {
        SysAuditServiceClient client = auditClientProvider.getIfAvailable();
        return client != null ? new RemoteSysAuditLogService(client) : new LoggingSysAuditLogService();
    }

    @Bean
    @ConditionalOnMissingBean(SysAuditLogAspect.class)
    public SysAuditLogAspect sysAuditLogAspect(ISysAuditLogService auditLogService, ObjectMapper objectMapper) {
        return new SysAuditLogAspect(auditLogService, objectMapper);
    }
}
