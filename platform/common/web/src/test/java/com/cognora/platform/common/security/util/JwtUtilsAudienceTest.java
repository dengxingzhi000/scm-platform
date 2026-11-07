package com.cognora.platform.common.security.util;

import com.cognora.platform.common.exception.UnauthorizedException;
import com.cognora.platform.common.security.audience.TokenAudience;
import com.cognora.platform.common.security.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

/**
 * 令牌受众（aud）签发与解析测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Audience Tests")
class JwtUtilsAudienceTest {

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private JwtUtils jwtUtils;

    private static final String VALID_SECRET = "dGhpc19pc19hX3Zlcnlfc2VjdXJlX3NlY3JldF9rZXlfdGhhdF9pc19hdF9sZWFzdF81MTJfYml0c19sb25nX2Zvcl9obWFjX3NoYTUxMl90b19mdW5jdGlvbl9wcm9wZXJseQ==";

    private UUID userId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        lenient().when(jwtProperties.getSecret()).thenReturn(VALID_SECRET);
        lenient().when(jwtProperties.getExpiration()).thenReturn(3_600_000L);
        lenient().when(jwtProperties.getRefreshExpiration()).thenReturn(604_800_000L);
        lenient().when(jwtProperties.getIssuer()).thenReturn("audience-test");
        lenient().when(stringRedisTemplate.execute(
                        org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(null);
        jwtUtils.init();
    }

    @Test
    @DisplayName("访问令牌应携带 aud 声明")
    void accessTokenCarriesAudience() {
        String token = jwtUtils.generateAccessToken(userId, "admin", Set.of("ROLE_ADMIN"),
                Set.of("system:user:list"), "device-1", "10.0.0.1", List.of("pwd"),
                tenantId, "default", TokenAudience.ADMIN);

        assertThat(jwtUtils.getAudienceFromToken(token)).isEqualTo(TokenAudience.ADMIN);
        assertThat(jwtUtils.getTenantIdFromToken(token)).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("刷新令牌应携带 aud，轮换后可继续解析")
    void refreshTokenCarriesAudience() {
        String refreshToken = jwtUtils.generateRefreshToken(userId, "member", "device-2",
                tenantId, "default", TokenAudience.MALL);

        assertThat(jwtUtils.getAudienceFromToken(refreshToken)).isEqualTo(TokenAudience.MALL);
    }

    @Test
    @DisplayName("未指定受众时不写入 aud，供升级前调用方使用")
    void legacyTokenHasNoAudience() {
        String token = jwtUtils.generateAccessToken(userId, "legacy", Set.of("ROLE_USER"),
                Set.of(), "device-3", "10.0.0.2", List.of("pwd"), tenantId, "default");

        assertThat(jwtUtils.getAudienceFromToken(token)).isNull();
    }

    @Test
    @DisplayName("兼容窗口关闭后旧 refresh token 统一 401，强制重新登录")
    void legacyRefreshTokenIsRejected() {
        String legacyRefreshToken = jwtUtils.generateRefreshToken(
                userId, "legacy", "device-4", tenantId, "default");

        assertThatThrownBy(() -> jwtUtils.refreshToken(
                legacyRefreshToken, Set.of("ROLE_USER"), Set.of(), "device-4", "10.0.0.3"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("invalid_token_audience_missing");

        assertThatThrownBy(() -> jwtUtils.refreshTokenWithRotation(
                legacyRefreshToken, Set.of("ROLE_USER"), Set.of(), "device-4", "10.0.0.3"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("invalid_token_audience_missing");
    }
}
