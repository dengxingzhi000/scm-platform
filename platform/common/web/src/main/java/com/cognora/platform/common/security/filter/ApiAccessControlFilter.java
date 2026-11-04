package com.cognora.platform.common.security.filter;

import com.cognora.platform.common.security.PermissionService;
import com.cognora.platform.common.security.PermissionService.PermissionServiceException;
import com.cognora.platform.common.log.enums.SecurityEventType;
import com.cognora.platform.common.log.service.ISysAuditLogService;
import com.cognora.platform.common.security.config.ApiAccessControlProperties;
import com.cognora.platform.common.security.metrics.SecurityMetrics;
import com.cognora.platform.common.security.util.FilterBypassHelper;
import com.cognora.platform.common.security.util.IpUtils;
import com.cognora.platform.common.security.util.RequestPathMatcher;
import com.cognora.platform.common.security.util.SecurityErrorResponseWriter;
import com.cognora.platform.common.web.domain.SecurityUser;
import com.cognora.platform.common.web.util.SecurityUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * API访问控制过滤器：URL/Method 精细化权限校验，支持白名单、旁路和 Sentinel 熔断保护
 *
 * <p>REFACTORED: Now depends on PermissionService interface (common/core)
 * instead of PermissionAccessPort. This decouples from business modules.
 *
 * <p>使用 Sentinel 进行熔断降级保护，替代原有的 SimpleCircuitBreaker
 *
 * <p>Sentinel Resource: "api-access-control"
 *
 * @author Deng
 * @version 2.0 - Refactored to use PermissionService
 */
@Component
@Slf4j
@RequiredArgsConstructor
@NullMarked
public class ApiAccessControlFilter extends OncePerRequestFilter {
    private final PermissionService permissionService;
    private final ObjectProvider<ISysAuditLogService> auditLogProvider;
    private final SecurityMetrics securityMetrics;
    private final ApiAccessControlProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        if (isWhitelisted(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Configured exemptions are public or protected by another mechanism (internal HMAC
        // signature, payment channel verification). They must not go through a permission lookup.
        if (isExempt(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // A verified service signature already authorised this request; do not demand a permission.
        if (Boolean.TRUE.equals(request.getAttribute(
                InternalEndpointSignatureFilter.SIGNATURE_VERIFIED_ATTRIBUTE))) {
            filterChain.doFilter(request, response);
            return;
        }

        SecurityUser principal = SecurityUtils.getCurrentUser();
        UUID userId = principal != null ? principal.getUserId() : SecurityUtils.getCurrentUserUuid().orElse(null);
        if (userId == null) {
            // 未登录，由JwtAuthenticationFilter 处理
            filterChain.doFilter(request, response);
            return;
        }

        if (shouldBypass(requestUri, principal)) {
            String reason = "api-access-bypass";
            markBypass(response, reason);
            securityMetrics.increment("security.access.bypass.config");
            filterChain.doFilter(request, response);
            return;
        }

        // Permission lookup is fail-closed. A discovery/transport failure answers 503 here
        // because exceptions raised in the filter chain bypass @RestControllerAdvice.
        List<String> requiredPermissions;
        try {
            requiredPermissions = permissionService.findPermissionsByUrl(requestUri, method);
        } catch (PermissionServiceException ex) {
            writePermissionServiceUnavailable(request, response, requestUri, method, ex);
            return;
        }

        if (requiredPermissions.isEmpty()) {
            handleUnregisteredEndpoint(request, response, userId, requestUri, method, filterChain);
            return;
        }

        Set<String> userPermissions;
        try {
            userPermissions = permissionService.findAllPermissionsByUserId(userId);
        } catch (PermissionServiceException ex) {
            writePermissionServiceUnavailable(request, response, requestUri, method, ex);
            return;
        }

        boolean hasPermission = requiredPermissions.stream()
                .anyMatch(userPermissions::contains);

        if (!hasPermission) {
            String username = SecurityUtils.getCurrentUsername().orElse(null);
            String ipAddress = IpUtils.getClientIp(request);

            ISysAuditLogService auditLogService = auditLogProvider.getIfAvailable();
            if (auditLogService != null) {
                auditLogService.recordSecurityEvent(
                        SecurityEventType.UNAUTHORIZED_ACCESS.name(),
                        SecurityEventType.UNAUTHORIZED_ACCESS.getRiskLevel(),
                        userId,
                        username,
                        ipAddress,
                        requestUri,
                        false,
                        "Attempted to access unauthorized API: " + method + " " + requestUri
                );
            }

            String traceId = request.getHeader("X-Request-ID");
            log.warn("Unauthorized API access: traceId={}, userId={}, user={}, uri={}, method={}, required={}",
                    (traceId + "").replace("\r", "_").replace("\n", "_"), (userId + "").replace("\r", "_").replace("\n", "_"), (username + "").replace("\r", "_").replace("\n", "_"), (requestUri + "").replace("\r", "_").replace("\n", "_"), (method + "").replace("\r", "_").replace("\n", "_"), (requiredPermissions + "").replace("\r", "_").replace("\n", "_"));

            securityMetrics.increment("security.access.denied");
            SecurityErrorResponseWriter.write(request, response, HttpServletResponse.SC_FORBIDDEN, "ACCESS_DENIED",
                    "You do not have permission to access this resource");
            return;
        }

        if (isSensitiveOperation(method, requestUri)) {
            logSensitiveOperation(userId, method, requestUri);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * No permission row matches the request URL/method. {@code REPORT_ONLY} keeps the historical
     * pass-through behaviour and records the endpoint so the catalog can be completed before
     * switching to {@code DENY}, which answers 403 with {@code API_NOT_REGISTERED}.
     */
    private void handleUnregisteredEndpoint(HttpServletRequest request, HttpServletResponse response,
                                            UUID userId, String requestUri, String method,
                                            FilterChain filterChain) throws ServletException, IOException {
        String traceId = sanitize(request.getHeader("X-Request-ID"));
        if (properties.getUnregisteredEndpointPolicy()
                == ApiAccessControlProperties.UnregisteredEndpointPolicy.REPORT_ONLY) {
            securityMetrics.increment("security.access.unregistered_endpoint_report");
            log.warn("Unregistered API endpoint (report-only): traceId={}, userId={}, uri={}, method={}",
                    traceId, sanitize(userId + ""), sanitize(requestUri), sanitize(method));
            filterChain.doFilter(request, response);
            return;
        }

        securityMetrics.increment("security.access.unregistered_endpoint_denied");
        log.warn("Unregistered API endpoint denied: traceId={}, userId={}, uri={}, method={}",
                traceId, sanitize(userId + ""), sanitize(requestUri), sanitize(method));
        SecurityErrorResponseWriter.write(request, response, HttpServletResponse.SC_FORBIDDEN,
                "API_NOT_REGISTERED",
                "This API endpoint is not registered in the permission catalog");
    }

    @PostConstruct
    void logEffectiveRules() {
        if (!properties.isLogEffectiveRulesOnStartup()) {
            return;
        }
        log.warn("API access control effective rules: enabled={}, unregisteredEndpointPolicy={}, "
                        + "whitelist={}, exemptPaths={}, exemptPathExclusions={}, "
                        + "internalSignaturePaths={}, signatureOrPermissionPaths={}",
                properties.isEnabled(), properties.getUnregisteredEndpointPolicy(),
                properties.getWhitelist(), properties.getExemptPaths(),
                properties.getExemptPathExclusions(),
                properties.getInternalSignaturePaths(), properties.getSignatureOrPermissionPaths());
    }

    private boolean isWhitelisted(String uri) {
        return FilterBypassHelper.matchesAny(uri, properties.getWhitelist());
    }

    private boolean isExempt(String uri) {
        String normalized = RequestPathMatcher.normalizePath(uri);
        if (normalized == null) {
            return false;
        }
        if (RequestPathMatcher.matchesNormalized(normalized, properties.getExemptPathExclusions())) {
            return false;
        }
        return RequestPathMatcher.matchesNormalized(normalized, properties.getExemptPaths());
    }

    private boolean shouldBypass(String uri, SecurityUser principal) {
        return FilterBypassHelper.shouldBypass(uri, principal, properties.getBypassPaths(), properties.getBypassUsers(),
                properties.getBypassRoles(), properties.getBypassPermissions());
    }

    private boolean isSensitiveOperation(String method, String uri) {
        if ("DELETE".equals(method)) {
            return true;
        }

        String[] sensitiveKeywords = {
                "delete", "reset", "password", "grant", "revoke",
                "approve", "reject", "lock", "unlock"
        };

        String lowerUri = uri.toLowerCase();
        for (String keyword : sensitiveKeywords) {
            if (lowerUri.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private void logSensitiveOperation(UUID userId, String method, String uri) {
        String username = SecurityUtils.getCurrentUsername().orElse(null);
        log.info("Sensitive operation: userId={}, user={}, method={}, uri={}",
                (userId + "").replace("\r", "_").replace("\n", "_"), (username + "").replace("\r", "_").replace("\n", "_"), (method + "").replace("\r", "_").replace("\n", "_"), (uri + "").replace("\r", "_").replace("\n", "_"));
    }

    private void markBypass(HttpServletResponse response, String reason) {
        response.setHeader("X-Security-Bypass", reason);
    }

    private void writePermissionServiceUnavailable(HttpServletRequest request, HttpServletResponse response,
                                                    String requestUri, String method, PermissionServiceException ex)
            throws IOException {
        securityMetrics.increment("security.access.permission_service_unavailable");
        log.error("Permission service unavailable: traceId={}, uri={}, method={}, reason={}",
                sanitize(request.getHeader("X-Request-ID")), sanitize(requestUri), sanitize(method),
                sanitize(ex.getMessage()));
        SecurityErrorResponseWriter.write(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "PERMISSION_SERVICE_UNAVAILABLE",
                "Permission service is temporarily unavailable; access denied as a safety measure");
    }

    private static String sanitize(String value) {
        return (value + "").replace("\r", "_").replace("\n", "_");
    }
}