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
            // 会话自助端点：目标用户只从访问令牌解析，控制器拒绝任何 userId 入参，
            // 因此不需要角色权限码；强制登记反而会挡住普通用户管理自己的会话。
            "/api/auth/sessions",
            "/api/auth/sessions/**",
            // 个人资料自助端点：userId 只从令牌取，字段白名单由 DTO 约束。
            "/api/system/users/me",
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
            "/error",
            // 自助端点：登录用户操作自己的资源，归属校验由控制器内的 OwnershipGuard 完成
            // (PR-C)。它们不承担角色权限语义，强制登记反而会挡住普通会员。
            "/api/v1/mall/cart",
            "/api/v1/mall/cart/**",
            "/api/v1/mall/products/**/reviews",
            "/api/v1/mall/reviews",
            "/api/v1/mall/reviews/**",
            "/api/v1/members/**",
            "/api/v1/promotions/**",
            "/sys-user-notification",
            "/sys-user-notification/**",
            "/sys-user-notification-preference",
            "/sys-user-notification-preference/**",
            // 公开检索（无需权限）；个人化结果内部按登录态取 userId 并做归属校验
            "/api/v1/search/products",
            "/api/v1/search/products/**",
            // 支付自助操作：支付单/退款单的所有者由登录态决定，审批类端点仍在权限目录内
            "/api/v1/payments/create",
            "/api/v1/payments/*",
            "/api/v1/payments/*/cancel",
            "/api/v1/payments/refunds",
            "/api/v1/payments/refunds/*"
    ));

    /**
     * 显式排除项：即使命中 {@link #exemptPaths} 的 Ant 通配符，也仍然走权限校验。
     *
     * <p>用于解决同一层级通配符无法区分「变量段」与「字面量段」的冲突，例如
     * {@code /api/v1/payments/*} 会匹配 {@code /api/v1/payments/page}，但分页属于后台
     * 管理端点，必须保留权限校验。
     */
    private List<String> exemptPathExclusions = new ArrayList<>(List.of(
            "/api/v1/payments/page",
            "/api/v1/payments/refunds/page"
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

    /**
     * 已登录用户访问「权限目录中不存在的 URL」时的处置策略。
     *
     * <p>{@link UnregisteredEndpointPolicy#REPORT_ONLY} 只记录 WARN 与指标，不拦截，用于切换
     * 默认拒绝前的观测期；{@link UnregisteredEndpointPolicy#DENY} 返回 403 与
     * {@code API_NOT_REGISTERED}。默认 DENY：上线前已确认存量端点全部登记或显式豁免。
     */
    private UnregisteredEndpointPolicy unregisteredEndpointPolicy = UnregisteredEndpointPolicy.DENY;

    public enum UnregisteredEndpointPolicy {
        REPORT_ONLY,
        DENY
    }

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
