package com.scmcloud.common.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.List;

/**
 * {@code scm.tenant.*}. Records bind through the constructor, so every component is
 * boxed and defaulted in the compact constructor — an absent key keeps the previous
 * behaviour instead of silently turning isolation off.
 */
@ConfigurationProperties(prefix = "scm.tenant")
public record TenantProperties(
        Boolean failOnParseError,
        Boolean required,
        List<String> excludePaths,
        List<String> excludeTables,
        Boolean sqlIsolation) {

    public static final List<String> DEFAULT_EXCLUDE_PATHS =
            List.of("/actuator/**", "/v3/api-docs/**");

    // Explicit because the convenience constructor below makes the choice ambiguous.
    @ConstructorBinding
    public TenantProperties {
        failOnParseError = failOnParseError == null || failOnParseError;
        required = required == null || required;
        sqlIsolation = sqlIsolation == null || sqlIsolation;
        excludePaths = excludePaths == null || excludePaths.isEmpty()
                ? DEFAULT_EXCLUDE_PATHS
                : List.copyOf(excludePaths);
        excludeTables = excludeTables == null ? List.of() : List.copyOf(excludeTables);
    }

    /** Kept for tests and manual wiring: framework defaults, no extra excluded tables. */
    public TenantProperties(boolean failOnParseError, boolean required, List<String> excludePaths) {
        this(failOnParseError, required, excludePaths, List.of(), true);
    }
}
