package com.cognora.platform.auth.audience;

import com.cognora.platform.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 登录 clientId 到令牌受众的映射测试。
 */
@DisplayName("Auth audience resolver tests")
class AuthAudienceResolverTest {

    private final AuthClientProperties properties = new AuthClientProperties();
    private final AuthAudienceResolver resolver = new AuthAudienceResolver(properties);

    @Test
    @DisplayName("管理台 client 映射为 cognora-admin")
    void adminClientMapsToAdminAudience() {
        AuthAudienceResolver.ResolvedClient resolved = resolver.resolve("cognora-aura-web");

        assertThat(resolved.clientId()).isEqualTo("cognora-aura-web");
        assertThat(resolved.audience()).isEqualTo("cognora-admin");
    }

    @Test
    @DisplayName("商城 client 映射为 cognora-mall")
    void mallClientMapsToMallAudience() {
        AuthAudienceResolver.ResolvedClient resolved = resolver.resolve("cognora-aura-mall");

        assertThat(resolved.audience()).isEqualTo("cognora-mall");
    }

    @Test
    @DisplayName("未传 clientId 时回落到管理台，兼容升级前的前端")
    void blankClientIdFallsBackToDefault() {
        AuthAudienceResolver.ResolvedClient resolved = resolver.resolve("  ");

        assertThat(resolved.clientId()).isEqualTo("cognora-aura-web");
        assertThat(resolved.audience()).isEqualTo("cognora-admin");
    }

    @Test
    @DisplayName("未登记的 clientId 直接拒绝")
    void unknownClientIsRejected() {
        assertThatThrownBy(() -> resolver.resolve("rogue-client"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("unknown_client_id");
    }
}
