package com.cognora.platform.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "cognora.security.api-filter")
@Data
public class ApiAccessControlProperties {

    private boolean enabled = true;
    private List<String> whitelist = new ArrayList<>(List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/actuator/health"
    ));

    /**
     * Endpoints that never need a permission lookup: public authentication entry points,
     * internally signed service-to-service channels, payment channels and framework endpoints.
     * Values are Ant-style patterns ({@code **} wildcard) or exact paths, matched after
     * normalising the request path (matrix parameters, percent decoding, trailing slash).
     */
    private List<String> exemptPaths = new ArrayList<>(List.of(
            "/api/auth/logout",
            "/oauth2/logout",
            "/api/auth/userinfo",
            "/api/auth/webauthn/credentials/health",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/sys-audit-log",
            "/api/v1/inventory/reservation/**",
            "/sys-notification-audit/**",
            "/api/v1/payments",
            "/api/v1/refunds",
            "/api/v1/payments/callback/**",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/api/public/**",
            "/error"
    ));

    /**
     * Endpoints that must carry a valid service-to-service HMAC signature. Entries without a
     * {@code *} wildcard are exact paths; {@code **} entries are Ant patterns.
     */
    private List<String> internalSignaturePaths = new ArrayList<>(List.of(
            "/api/v1/inventory/reservation/**",
            "/sys-notification-audit/**",
            "/api/v1/payments",
            "/api/v1/refunds"
    ));

    /**
     * 双身份端点：满足其一即可放行——合法服务间签名，或当前登录用户持有
     * {@link #signatureOrPermissionCode}。用于搜索索引这类既有后台管理页直呼、
     * 又存在服务间调用方的端点。两者都不满足 → 403。
     */
    private List<String> signatureOrPermissionPaths = new ArrayList<>(List.of(
            "/api/v1/search/index/**"
    ));

    /** 双身份端点的前端权限码（需在 sys_permission 登记，见 seed 021）。 */
    private String signatureOrPermissionCode = "search:index:manage";

    private boolean logEffectiveRulesOnStartup = true;
    private List<String> bypassPaths = new ArrayList<>();
    private List<String> bypassRoles = new ArrayList<>();
    private List<String> bypassPermissions = new ArrayList<>();
    private List<String> bypassUsers = new ArrayList<>();
    private CircuitBreakerProperties circuitBreaker = new CircuitBreakerProperties();

    @Data
    public static class CircuitBreakerProperties {
        private boolean enabled = true;
        private int failureThreshold = 3;
        private Duration openDuration = Duration.ofSeconds(60);
        private boolean bypassOnOpen = true;
        private boolean forceOpen = false;
    }
}
