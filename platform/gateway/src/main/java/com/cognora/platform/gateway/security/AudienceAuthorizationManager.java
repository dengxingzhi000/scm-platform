package com.cognora.platform.gateway.security;

import com.cognora.platform.common.security.audience.TokenAudience;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * 网关的令牌受众校验。
 *
 * <p>在已登录（JWT 校验通过）之后按 {@code aud} 与请求路径判定：</p>
 * <ul>
 *   <li>{@code cognora-admin}：可访问除商城专属路径外的全部接口；</li>
 *   <li>{@code cognora-mall}：仅可访问商城前台白名单；</li>
 *   <li>没有 {@code aud} 的旧令牌：按 {@code security.audience.legacy-token-policy} 放行或拒绝；</li>
 *   <li>未知受众：拒绝，避免出现未登记的入口。</li>
 * </ul>
 *
 * @author Deng
 * @since 2026-11-06
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AudienceAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final AudiencePolicyProperties properties;

    @Override
    public Mono<AuthorizationResult> authorize(Mono<Authentication> authentication, AuthorizationContext context) {
        if (!properties.isEnabled()) {
            return Mono.<AuthorizationResult>just(new AuthorizationDecision(true));
        }

        String path = context.getExchange().getRequest().getPath().pathWithinApplication().value();
        return authentication
                .filter(Authentication::isAuthenticated)
                .map(auth -> (AuthorizationResult) decide(auth, path))
                .defaultIfEmpty(new AuthorizationDecision(false));
    }

    private AuthorizationDecision decide(Authentication authentication, String path) {
        String audience = extractAudience(authentication);

        if (!StringUtils.hasText(audience)) {
            if (properties.getLegacyTokenPolicy() == AudiencePolicyProperties.LegacyTokenPolicy.ALLOW) {
                log.debug("Legacy token without aud accepted: path={}", path);
                return new AuthorizationDecision(true);
            }
            log.warn("Token without aud rejected after enforcement: path={}", path);
            return new AuthorizationDecision(false);
        }

        if (TokenAudience.ADMIN.equals(audience)) {
            boolean denied = matchesAny(path, properties.getAdminDeniedPaths());
            if (denied) {
                log.warn("Admin token rejected on storefront path: path={}", path);
            }
            return new AuthorizationDecision(!denied);
        }

        if (TokenAudience.MALL.equals(audience)) {
            boolean allowed = matchesAny(path, properties.getMallAllowedPaths());
            if (!allowed) {
                log.warn("Mall token rejected on back-office path: path={}", path);
            }
            return new AuthorizationDecision(allowed);
        }

        log.warn("Token with unknown audience rejected: audience={}, path={}", audience, path);
        return new AuthorizationDecision(false);
    }

    private static boolean matchesAny(String path, Collection<String> patterns) {
        return patterns.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private static String extractAudience(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return null;
        }
        Object audience = jwtAuthentication.getToken().getClaim("aud");
        if (audience instanceof Collection<?> values) {
            for (Object value : values) {
                if (value != null && !value.toString().isBlank()) {
                    return value.toString();
                }
            }
            return null;
        }
        return audience == null || audience.toString().isBlank() ? null : audience.toString();
    }
}
