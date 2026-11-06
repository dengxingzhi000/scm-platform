package com.cognora.platform.common.security.filter;

import com.cognora.platform.common.rest.signature.InternalSignatureVerifier;
import com.cognora.platform.common.security.PermissionService;
import com.cognora.platform.common.security.config.ApiAccessControlProperties;
import com.cognora.platform.common.security.metrics.SecurityMetrics;
import com.cognora.platform.common.security.util.RequestPathMatcher;
import com.cognora.platform.common.security.util.SecurityErrorResponseWriter;
import com.cognora.platform.common.web.domain.SecurityUser;
import com.cognora.platform.common.web.util.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Enforces the service-to-service HMAC signature on the internal endpoints configured in
 * {@code cognora.security.api-filter.internal-signature-paths}.
 *
 * <p>Endpoints listed in {@code signature-or-permission-paths} accept either a valid signature
 * or the logged-in user holding {@code signature-or-permission-code} (default
 * {@code search:index:manage}); anything else is rejected. When a signature is accepted the
 * request attribute {@link #SIGNATURE_VERIFIED_ATTRIBUTE} is set so
 * {@link ApiAccessControlFilter} skips its own permission lookup.
 *
 * <p>Only configured paths are touched; every other request passes straight through. An invalid
 * or missing credential answers 403 with {@code ACCESS_DENIED} and increments
 * {@code security.internal.signature.rejected}. {@code /sys-audit-log} is not in the default list
 * because scm-audit already verifies it with {@code AuditWriteRequestGuard}.
 *
 * <p>This filter is ordered before {@link ApiAccessControlFilter} so that an accepted signature
 * cannot be rejected by the later permission check.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class InternalEndpointSignatureFilter extends OncePerRequestFilter {

    public static final String METRIC_REJECTED = "security.internal.signature.rejected";

    /** Request attribute marking a request whose service signature was already verified. */
    public static final String SIGNATURE_VERIFIED_ATTRIBUTE =
            "cognora.security.internal-signature-verified";

    private final InternalSignatureVerifier signatureVerifier;
    private final SecurityMetrics securityMetrics;
    private final ApiAccessControlProperties properties;
    private final PermissionService permissionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String normalized = RequestPathMatcher.normalizePath(request.getRequestURI());
        if (normalized == null) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean dualIdentity = RequestPathMatcher.matchesNormalized(
                normalized, properties.getSignatureOrPermissionPaths());
        if (dualIdentity) {
            if (signatureVerifier.hasValidSignature(request)) {
                request.setAttribute(SIGNATURE_VERIFIED_ATTRIBUTE, Boolean.TRUE);
                filterChain.doFilter(request, response);
                return;
            }
            if (hasRequiredPermission()) {
                filterChain.doFilter(request, response);
                return;
            }
            reject(request, response, "Internal endpoint requires a service signature or permission "
                    + properties.getSignatureOrPermissionCode());
            return;
        }

        if (!RequestPathMatcher.matchesNormalized(normalized, properties.getInternalSignaturePaths())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!signatureVerifier.hasValidSignature(request)) {
            reject(request, response, "Internal endpoint requires a valid service signature");
            return;
        }

        request.setAttribute(SIGNATURE_VERIFIED_ATTRIBUTE, Boolean.TRUE);
        filterChain.doFilter(request, response);
    }

    private boolean hasRequiredPermission() {
        SecurityUser user = SecurityUtils.getCurrentUser();
        UUID userId = user != null ? user.getUserId()
                : SecurityUtils.getCurrentUserUuid().orElse(null);
        if (userId == null) {
            return false;
        }
        if (user != null && user.getPermissions() != null
                && user.getPermissions().contains(properties.getSignatureOrPermissionCode())) {
            return true;
        }
        Set<String> permissions = permissionService.findAllPermissionsByUserId(userId);
        return permissions != null && permissions.contains(properties.getSignatureOrPermissionCode());
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        securityMetrics.increment(METRIC_REJECTED);
        log.error("Rejected internal request: appId={}, remote={}, uri={}, traceId={}",
                sanitize(request.getHeader("X-App-Id")), sanitize(request.getRemoteAddr()),
                sanitize(request.getRequestURI()), sanitize(request.getHeader("X-Request-ID")));
        SecurityErrorResponseWriter.write(request, response, HttpServletResponse.SC_FORBIDDEN,
                "ACCESS_DENIED", message);
    }

    private static String sanitize(String value) {
        return (value + "").replace("\r", "_").replace("\n", "_");
    }
}
