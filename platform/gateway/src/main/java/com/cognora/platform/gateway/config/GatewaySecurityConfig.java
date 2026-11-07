package com.cognora.platform.gateway.config;

import com.cognora.platform.gateway.security.AudienceAuthorizationManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Gateway OAuth2配置
 *
 * <p>注意：客户端实际通过 {@code /platform-auth/**}、{@code /platform-system/**}
 * 前缀访问网关（路由级 StripPrefix=1 后再转发到下游）。过渡期还支持前端仍在使用的
 * {@code /auth/**}、{@code /system/**}、{@code /common/**} 别名，因此放行路径
 * 同时覆盖规范前缀与别名前缀。
 *
 * <p>CORS 由本过滤器链承载（Spring Security 在认证之前处理 preflight），允许来源
 * 来自 {@code cognora.gateway.cors.allowed-origins}。
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder,
                                                            CorsConfigurationSource corsConfigurationSource,
                                                            AudienceAuthorizationManager audienceAuthorizationManager) {
        http
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeExchange(exchanges -> exchanges
                        // 认证服务的登录/刷新/OAuth2 授权端点（登录本身不能要求已认证）
                        .pathMatchers(
                                "/platform-auth/api/auth/login",
                                "/platform-auth/api/auth/refresh",
                                "/platform-auth/oauth2/**",
                                "/platform-auth/api/public/**",
                                // Transitional aliases used by the SPA before it moves
                                // to the canonical /platform-* prefixes.
                                "/auth/api/auth/login",
                                "/auth/api/auth/refresh",
                                "/auth/oauth2/**",
                                "/auth/api/public/**",
                                // Channel callbacks carry no platform JWT; the payment
                                // service authenticates them with the channel signature.
                                "/ecom-payment/api/v1/payments/callback/**",
                                // Carrier logistics callbacks authenticate with the LMS
                                // HMAC signature (timestamp + nonce) instead of the JWT.
                                "/lms-tms/api/v1/tms-tracking/callback/**"
                        ).permitAll()
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        // authenticated() 已包含在受众管理器内；这里额外按 aud 判定后台/商城路径。
                        .anyExchange().access(audienceAuthorizationManager)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable);

        return http.build();
    }

    /**
     * 访问令牌由 scm-auth 的 JwtUtils 用共享的 jwt.secret 以 HS512 签发；授权服务器的
     * JWKS 只覆盖 OAuth2 客户端，用它校验会报 "Unsupported algorithm of HS512"。
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
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
