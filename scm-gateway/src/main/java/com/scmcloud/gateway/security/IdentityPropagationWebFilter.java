package com.scmcloud.gateway.security;

import com.scmcloud.gateway.properties.IdentityPropagationProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 身份传播过滤器
 *
 * <p>认证成功后，将签名的身份信息添加到下游请求头中
 *
 * @author deng
 */
@Component
@RequiredArgsConstructor
public class IdentityPropagationWebFilter implements WebFilter, Ordered {

    private final IdentityPropagationProperties properties;
    private final IdentityTokenEncoder tokenEncoder;

    private final Counter propagationCounter;
    private final Counter skipCounter;

    public IdentityPropagationWebFilter(IdentityPropagationProperties properties,
                                        IdentityTokenEncoder tokenEncoder,
                                        MeterRegistry meterRegistry) {
        this.properties = properties;
        this.tokenEncoder = tokenEncoder;
        this.propagationCounter = meterRegistry.counter("gateway.identity.propagation.success");
        this.skipCounter = meterRegistry.counter("gateway.identity.propagation.skip");
    }

    @Override
    public int getOrder() {
        return SecurityWebFiltersOrder.AUTHORIZATION.getOrder() + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> propagate(exchange, chain, auth))
                .switchIfEmpty(Mono.defer(() -> {
                    skipCounter.increment();
                    return chain.filter(exchange);
                }));
    }

    private Mono<Void> propagate(ServerWebExchange exchange, WebFilterChain chain,
                                 JwtAuthenticationToken authentication) {
        Jwt token = authentication.getToken();
        String userId = getClaim(token, properties.getUserIdClaim());
        if (!StringUtils.hasText(userId)) {
            skipCounter.increment();
            return chain.filter(exchange);
        }

        String username = getClaim(token, properties.getUsernameClaim());
        String deviceId = getClaim(token, properties.getDeviceIdClaim());
        List<String> authorities = extractAuthorities(authentication);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", token.getSubject());
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("deviceId", deviceId);
        payload.put("authorities", authorities);
        payload.put("issuedAt", Instant.now().getEpochSecond());

        String identityToken = tokenEncoder.encode(payload);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.set(properties.getIdentityTokenHeader(), identityToken);
                    headers.set(properties.getUserIdHeader(), userId);
                    if (StringUtils.hasText(username)) {
                        headers.set(properties.getUsernameHeader(), username);
                    }
                    if (StringUtils.hasText(deviceId)) {
                        headers.set(properties.getDeviceIdHeader(), deviceId);
                    }
                    if (!authorities.isEmpty()) {
                        headers.set(properties.getRolesHeader(), String.join(",", authorities));
                    }
                })
                .build();

        propagationCounter.increment();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private List<String> extractAuthorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String getClaim(Jwt token, String claimName) {
        if (!StringUtils.hasText(claimName)) {
            return null;
        }
        Object value = token.getClaims().get(claimName);
        return value != null ? value.toString() : null;
    }
}
