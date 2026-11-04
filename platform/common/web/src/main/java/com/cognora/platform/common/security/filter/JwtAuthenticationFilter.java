package com.cognora.platform.common.security.filter;

import com.cognora.platform.common.security.metrics.SecurityMetrics;
import com.cognora.platform.common.security.util.AuthTokenContext;
import com.cognora.platform.common.security.util.HttpServletRequestUtils;
import com.cognora.platform.common.security.util.IpUtils;
import com.cognora.platform.common.security.util.JwtUtils;
import com.cognora.platform.common.security.util.SecurityErrorResponseWriter;
import com.cognora.platform.common.web.domain.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Jwt 杩囨护锟?
 *
 * @author Deng
 * createData 2025/10/11 13:49
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final HttpServletRequestUtils httpServletRequestUtils;
    private final SecurityMetrics securityMetrics;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, jakarta.servlet.ServletException {
        try {
            // 鑾峰彇 Token
            String token = httpServletRequestUtils.getTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                // 鑾峰彇褰撳墠璇锋眰淇℃伅
                String currentIp = IpUtils.getClientIp(request);
                String currentDeviceId = httpServletRequestUtils.getDeviceId(request);

                // 楠岃瘉 Token
                if (jwtUtils.validateToken(token, currentIp, currentDeviceId)) {
                    // Token 必须绑定租户，且与请求头租户一致；旧 Token（无租户声明）统一 401
                    JwtUtils.TenantBinding tenantBinding =
                            jwtUtils.validateTenantBinding(token, resolveTenantId(request));
                    if (tenantBinding == JwtUtils.TenantBinding.CLAIM_MISSING) {
                        securityMetrics.increment("security.jwt.tenant_missing");
                        SecurityErrorResponseWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                                "invalid_token_tenant_missing", "Token is missing tenant binding, please sign in again");
                        return;
                    }
                    if (tenantBinding == JwtUtils.TenantBinding.MISMATCH) {
                        securityMetrics.increment("security.jwt.tenant_mismatch");
                        SecurityErrorResponseWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                                "invalid_token_tenant_mismatch", "Token tenant does not match request tenant");
                        return;
                    }
                    AuthTokenContext.set(token, currentDeviceId);
                    // 鎻愬彇鐢ㄦ埛淇℃伅锛堝緱鐩婁簬ThreadLocal缂撳瓨锛岃繖4娆¤皟鐢ㄥ彧浼氳В鏋怲oken涓€娆★級
                    UUID userId = jwtUtils.getUserIdFromToken(token);
                    String username = jwtUtils.getUsernameFromToken(token);
                    Set<String> permissions = jwtUtils.getPermissionsFromToken(token);
                    Set<String> roles = jwtUtils.getRolesFromToken(token);
                    UUID tenantId = jwtUtils.getTenantIdFromToken(token);
                    String tenantCode = jwtUtils.getTenantCodeFromToken(token);

                    // 鏋勫缓鏉冮檺鍒楄〃
                    Set<SimpleGrantedAuthority> authorities = permissions.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toSet());

                    authorities.addAll(roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toSet()));

                    // 鍒涘缓璁よ瘉瀵硅薄
                    SecurityUser userDetails = SecurityUser.builder()
                            .userId(userId)
                            .username(username)
                            .permissions(permissions)
                            .roles(roles)
                            .tenantId(tenantId)
                            .tenantCode(tenantCode)
                            .build();

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, authorities);

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 璁剧疆锟絊ecurity涓婁笅锟?
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("User authenticated: traceId={}, userId={}, username={}",
                            (request.getHeader("X-Request-ID") + "").replace("\r", "_")
                                    .replace("\n", "_"), (userId + "").replace("\r", "_")
                                    .replace("\n", "_"), (username + "").replace("\r", "_")
                                    .replace("\n", "_"));
                } else {
                    securityMetrics.increment("security.jwt.invalid");
                    SecurityErrorResponseWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                            "INVALID_TOKEN", "Token validation failed");
                    return;
                }
            }

        } catch (Exception e) {
            securityMetrics.increment("security.jwt.errors");
            log.error("Cannot set user authentication traceId={}", (request.getHeader("X-Request-ID") + "").replace("\r", "_").replace("\n", "_"), e);
            SecurityErrorResponseWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_ERROR",
                    "Authentication error");
            return;
        } finally {
            // 娓呯悊ThreadLocal缂撳瓨锛岄槻姝㈠唴瀛樻硠锟?
            JwtUtils.clearTokenCache();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            AuthTokenContext.clear();
        }
    }

    /**
     * 与 TenantFilter 相同的租户来源顺序：X-Tenant-Id → Tenant-Id → tenantId 查询参数。
     */
    private static String resolveTenantId(HttpServletRequest request) {
        String tenantId = request.getHeader("X-Tenant-Id");
        if (!StringUtils.hasText(tenantId)) {
            tenantId = request.getHeader("Tenant-Id");
        }
        if (!StringUtils.hasText(tenantId)) {
            tenantId = request.getParameter("tenantId");
        }
        return tenantId;
    }
}
