package com.scmcloud.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Gateway OAuth2配置
 *
 * <p>注意：客户端实际通过 {@code /auth/**}、{@code /system/**}、{@code /common/**}
 * 前缀访问网关（路由级 StripPrefix=1 后再转发到下游），因此在网关层放行的路径
 * 必须与这些真实前缀对齐。
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    /**
     * 认证服务 JWKS 地址（可配置，避免硬编码 K8s 内部服务名）。
     * 默认指向 auth-service 内部地址，本地环境通过 application-local.yaml 覆盖。
     */
    @Value("${security.oauth2.resource-server.jwk-set-uri:http://auth-service:8106/oauth2/jwks}")
    private String jwkSetUri;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchanges -> exchanges
                        // 认证服务的登录/刷新/OAuth2 授权端点（登录本身不能要求已认证）
                        .pathMatchers(
                                "/auth/api/auth/login",
                                "/auth/api/auth/refresh",
                                "/auth/oauth2/**",
                                "/auth/api/public/**"
                        ).permitAll()
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwkSetUri(jwkSetUri)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable);

        return http.build();
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return new ReactiveJwtAuthenticationConverterAdapter(jwtConverter);
    }
}
