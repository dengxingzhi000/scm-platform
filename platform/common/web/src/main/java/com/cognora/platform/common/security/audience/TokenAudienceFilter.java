package com.cognora.platform.common.security.audience;

import com.cognora.platform.common.security.metrics.SecurityMetrics;
import com.cognora.platform.common.security.util.HttpServletRequestUtils;
import com.cognora.platform.common.security.util.JwtUtils;
import com.cognora.platform.common.security.util.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 业务服务侧的令牌受众校验。
 *
 * <p>在 {@code JwtAuthenticationFilter} 完成签名、租户、设备校验之后再判断 {@code aud}：
 * 受众不在 {@code security.audience.allowed} 中返回 403；旧令牌没有 {@code aud} 时按
 * {@code security.audience.allow-legacy-token} 决定放行还是 401，兼容窗口内默认放行。</p>
 *
 * @author Deng
 * @since 2026-11-06
 */
@Component
@Slf4j
@NullMarked
@RequiredArgsConstructor
public class TokenAudienceFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final HttpServletRequestUtils httpServletRequestUtils;
    private final TokenAudienceProperties properties;
    private final SecurityMetrics securityMetrics;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        if (!properties.isEnabled() || properties.getAllowed().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = httpServletRequestUtils.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            // 未携带令牌的请求交给 Spring Security 的入口点处理。
            filterChain.doFilter(request, response);
            return;
        }

        String audience = jwtUtils.getAudienceFromToken(token);
        if (!StringUtils.hasText(audience)) {
            if (properties.isAllowLegacyToken()) {
                securityMetrics.increment("security.audience.legacy_allowed");
                log.debug("Legacy token without aud claim accepted: uri={}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }
            securityMetrics.increment("security.audience.missing");
            SecurityErrorResponseWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                    "invalid_token_audience_missing", "Token is missing audience binding, please sign in again");
            return;
        }

        if (!properties.getAllowed().contains(audience)) {
            securityMetrics.increment("security.audience.denied");
            log.warn("Token audience not allowed: audience={}, uri={}", audience, request.getRequestURI());
            SecurityErrorResponseWriter.write(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "AUDIENCE_NOT_ALLOWED", "This token audience cannot access this service");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
