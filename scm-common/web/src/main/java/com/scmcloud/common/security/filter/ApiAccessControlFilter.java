package com.scmcloud.common.security.filter;

import com.scmcloud.common.security.PermissionService;
import com.scmcloud.common.log.enums.SecurityEventType;
import com.scmcloud.common.log.service.ISysAuditLogService;
import com.scmcloud.common.security.config.ApiAccessControlProperties;
import com.scmcloud.common.security.metrics.SecurityMetrics;
import com.scmcloud.common.security.util.FilterBypassHelper;
import com.scmcloud.common.security.util.IpUtils;
import com.scmcloud.common.security.util.SecurityErrorResponseWriter;
import com.scmcloud.common.web.domain.SecurityUser;
import com.scmcloud.common.web.util.SecurityUtils;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * API璁块棶鎺у埗杩囨护鍣細URL/Method 绮剧粏鍖栨潈闄愭牎楠岋紝鏀寔鐧藉悕鍗曘€佹梺璺拰 Sentinel 鐔旀柇锟?
 *
 * <p>REFACTORED: Now depends on PermissionService interface (common/core)
 * instead of PermissionAccessPort. This decouples from business modules.
 *
 * <p>浣跨敤 Sentinel 杩涜鐔旀柇闄嶇骇淇濇姢锛屾浛浠ｅ師鏈夌殑 SimpleCircuitBreaker
 *
 * <p>Sentinel Resource: "api-access-control"
 *
 * @author Deng
 * @version 2.0 - Refactored to use PermissionService
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiAccessControlFilter extends OncePerRequestFilter {
    private final PermissionService permissionService;
    private final ISysAuditLogService auditLogService;
    private final SecurityMetrics securityMetrics;
    private final ApiAccessControlProperties properties;

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {

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

        SecurityUser principal = SecurityUtils.getCurrentUser();
        UUID userId = principal != null ? principal.getUserId() : SecurityUtils.getCurrentUserUuid().orElse(null);
        if (userId == null) {
            // 鏈櫥褰曪紝锟絁wtAuthenticationFilter 澶勭悊
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

        // Permission lookup is protected by Sentinel in FeignPermissionAccess
        // If circuit is open, exception will be thrown and handled by SecurityRestExceptionHandler
        List<String> requiredPermissions = permissionService.findPermissionsByUrl(requestUri, method);

        if (requiredPermissions.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        Set<String> userPermissions = permissionService.findAllPermissionsByUserId(userId);

        boolean hasPermission = requiredPermissions.stream()
                .anyMatch(userPermissions::contains);

        if (!hasPermission) {
            String username = SecurityUtils.getCurrentUsername().orElse(null);
            String ipAddress = IpUtils.getClientIp(request);

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

            String traceId = request.getHeader("X-Request-ID");
            log.warn("Unauthorized API access: traceId={}, userId={}, user={}, uri={}, method={}, required={}",
                    traceId, userId, username, requestUri, method, requiredPermissions);

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

    private boolean isWhitelisted(String uri) {
        return FilterBypassHelper.matchesAny(uri, properties.getWhitelist());
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
        log.info("Sensitive operation: user={}, method={}, uri={}",
                username, method, uri);
        // TODO: 鍙互鍙戦€佸疄鏃跺憡锟?
    }

    private void markBypass(HttpServletResponse response, String reason) {
        response.setHeader("X-Security-Bypass", reason);
    }
}

