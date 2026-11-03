package com.scmcloud.common.access;

import com.scmcloud.common.rest.client.SysPermissionServiceClient;
import com.scmcloud.common.security.PermissionService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Registers the HTTP fallback {@link PermissionService} only when a service does not provide
 * its own (e.g. scm-system's Dubbo adapter). Declaring this on the class itself with
 * {@code @Component + @ConditionalOnMissingBean} always skipped it, because the condition
 * matched the bean being registered.
 */
@AutoConfiguration
@ConditionalOnClass({SysPermissionServiceClient.class, MeterRegistry.class})
public class PermissionAccessAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PermissionService.class)
    public FeignPermissionAccess feignPermissionAccess(SysPermissionServiceClient permissionServiceClient,
                                                       MeterRegistry meterRegistry) {
        return new FeignPermissionAccess(permissionServiceClient, meterRegistry);
    }
}
