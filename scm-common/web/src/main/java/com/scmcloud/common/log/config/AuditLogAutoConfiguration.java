package com.scmcloud.common.log.config;

import com.scmcloud.common.log.service.ISysAuditLogService;
import com.scmcloud.common.log.service.LoggingSysAuditLogService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AuditLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ISysAuditLogService.class)
    public ISysAuditLogService sysAuditLogService() {
        return new LoggingSysAuditLogService();
    }
}
