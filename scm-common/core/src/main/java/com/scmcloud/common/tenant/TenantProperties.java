package com.scmcloud.common.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "scm.tenant")
public record TenantProperties(
        boolean failOnParseError,
        boolean required,
        List<String> excludePaths) {
    public TenantProperties {
        if (excludePaths == null) {
            excludePaths = List.of();
        }
    }
}
