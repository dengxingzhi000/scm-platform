package com.cognora.platform.gateway.security;

import com.cognora.platform.common.security.audience.TokenAudience;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关受众与路径策略测试。
 */
@DisplayName("Gateway audience authorization tests")
class AudienceAuthorizationManagerTest {

    private final AudiencePolicyProperties properties = new AudiencePolicyProperties();
    private final AudienceAuthorizationManager manager = new AudienceAuthorizationManager(properties);

    @Test
    @DisplayName("商城令牌不能访问后台系统接口")
    void mallTokenCannotAccessSystemApi() {
        assertThat(decide("/platform-system/api/system/users", TokenAudience.MALL)).isFalse();
        assertThat(decide("/system/api/system/users", TokenAudience.MALL)).isFalse();
    }

    @Test
    @DisplayName("商城令牌不能访问 SCM 后台接口")
    void mallTokenCannotAccessScmApi() {
        assertThat(decide("/scm-order/api/v1/orders", TokenAudience.MALL)).isFalse();
        assertThat(decide("/scm-inventory/api/v1/inventory", TokenAudience.MALL)).isFalse();
    }

    @Test
    @DisplayName("商城令牌可以访问商城前台接口")
    void mallTokenCanAccessStorefrontApi() {
        assertThat(decide("/ecom-mall/api/v1/products", TokenAudience.MALL)).isTrue();
        assertThat(decide("/scm-mall/api/v1/products", TokenAudience.MALL)).isTrue();
        assertThat(decide("/scm-member/api/v1/members/me", TokenAudience.MALL)).isTrue();
    }

    @Test
    @DisplayName("管理台令牌不能访问商城专属入口")
    void adminTokenCannotAccessMallEntry() {
        assertThat(decide("/ecom-mall/api/v1/products", TokenAudience.ADMIN)).isFalse();
        assertThat(decide("/scm-mall/api/v1/products", TokenAudience.ADMIN)).isFalse();
    }

    @Test
    @DisplayName("管理台令牌可访问后台接口")
    void adminTokenCanAccessBackOffice() {
        assertThat(decide("/platform-system/api/system/users", TokenAudience.ADMIN)).isTrue();
        assertThat(decide("/scm-order/api/v1/orders", TokenAudience.ADMIN)).isTrue();
    }

    @Test
    @DisplayName("默认拒绝无 aud 的旧令牌（兼容窗口已关闭）")
    void legacyTokenIsDeniedByDefault() {
        assertThat(decide("/platform-system/api/system/users", null)).isFalse();
    }

    @Test
    @DisplayName("显式配置 ALLOW 时旧令牌放行（仅用于临时回滚）")
    void legacyTokenIsAllowedWhenExplicitlyConfigured() {
        properties.setLegacyTokenPolicy(AudiencePolicyProperties.LegacyTokenPolicy.ALLOW);

        assertThat(decide("/platform-system/api/system/users", null)).isTrue();
    }

    @Test
    @DisplayName("收紧后旧令牌被拒绝")
    void legacyTokenIsDeniedAfterEnforcement() {
        properties.setLegacyTokenPolicy(AudiencePolicyProperties.LegacyTokenPolicy.DENY);

        assertThat(decide("/platform-system/api/system/users", null)).isFalse();
    }

    @Test
    @DisplayName("未知受众一律拒绝")
    void unknownAudienceIsDenied() {
        assertThat(decide("/ecom-mall/api/v1/products", "other-audience")).isFalse();
    }

    @Test
    @DisplayName("未认证请求被拒绝")
    void unauthenticatedRequestIsDenied() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/platform-system/api/system/users").build());
        AuthorizationContext context = new AuthorizationContext(exchange);

        Boolean granted = manager.authorize(Mono.empty(), context)
                .map(decision -> decision.isGranted())
                .block();

        assertThat(granted).isFalse();
    }

    private boolean decide(String path, String audience) {
        Jwt.Builder jwtBuilder = Jwt.withTokenValue("token")
                .header("alg", "HS512")
                .subject("user");
        if (audience != null) {
            jwtBuilder.claim("aud", audience);
        }
        Jwt jwt = jwtBuilder.build();

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
        AuthorizationContext context = new AuthorizationContext(exchange);
        Boolean granted = manager.authorize(Mono.just(
                        new JwtAuthenticationToken(jwt, java.util.List.of(), "user")), context)
                .map(decision -> decision.isGranted())
                .block();

        return Boolean.TRUE.equals(granted);
    }
}
