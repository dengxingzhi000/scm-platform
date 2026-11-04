package com.cognora.platform.auth.security;

import com.cognora.platform.common.security.properties.JwtProperties;
import com.cognora.platform.common.security.util.JwtUtils;
import com.cognora.platform.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * P0-1：JWT 必须绑定租户，且校验时与请求头租户核对。
 */
class TenantBoundJwtTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();
    private static final String TENANT_A_CODE = "acme";

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        jwtUtils = new JwtUtils(properties, mock(RedisTemplate.class), mock(StringRedisTemplate.class));
        jwtUtils.init();
    }

    @Test
    void tokenWithMatchingTenantIsAccepted() {
        String token = jwtUtils.generateAccessToken(
                USER_ID, "alice", Set.of("ROLE_USER"), Set.of("order:view"), "device-1", "127.0.0.1",
                List.of("pwd"), TENANT_A, TENANT_A_CODE);

        assertEquals(JwtUtils.TenantBinding.VALID, jwtUtils.validateTenantBinding(token, TENANT_A.toString()));
        assertEquals(TENANT_A, jwtUtils.getTenantIdFromToken(token));
        assertEquals(TENANT_A_CODE, jwtUtils.getTenantCodeFromToken(token));
    }

    @Test
    void crossTenantTokenIsRejected() {
        String token = jwtUtils.generateAccessToken(
                USER_ID, "alice", Set.of("ROLE_USER"), Set.of("order:view"), "device-1", "127.0.0.1",
                List.of("pwd"), TENANT_A, TENANT_A_CODE);

        assertEquals(JwtUtils.TenantBinding.MISMATCH, jwtUtils.validateTenantBinding(token, TENANT_B.toString()));
    }

    @Test
    void requestWithoutTenantHeaderIsRejected() {
        String token = jwtUtils.generateAccessToken(
                USER_ID, "alice", Set.of("ROLE_USER"), Set.of("order:view"), "device-1", "127.0.0.1",
                List.of("pwd"), TENANT_A, TENANT_A_CODE);

        assertEquals(JwtUtils.TenantBinding.MISMATCH, jwtUtils.validateTenantBinding(token, null));
        assertEquals(JwtUtils.TenantBinding.MISMATCH, jwtUtils.validateTenantBinding(token, " "));
    }

    @Test
    void legacyTokenWithoutTenantClaimIsRejected() throws Exception {
        String legacyToken = legacyTokenWithoutTenantClaim();

        assertEquals(JwtUtils.TenantBinding.CLAIM_MISSING,
                jwtUtils.validateTenantBinding(legacyToken, TENANT_A.toString()));
        assertNull(jwtUtils.getTenantIdFromToken(legacyToken));
    }

    @Test
    void issuingWithoutTenantIsRefusedInsteadOfDowngrading() {
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> jwtUtils.generateAccessToken(USER_ID, "alice", Set.of("ROLE_USER"), Set.of("order:view"),
                        "device-1", "127.0.0.1", List.of("pwd"), null, null));
        assertEquals("invalid_token_tenant_missing", ex.getMessage());

        assertThrows(UnauthorizedException.class,
                () -> jwtUtils.generateRefreshToken(USER_ID, "alice", "device-1", null, null));
    }

    @Test
    void refreshTokenCarriesTenantSoRotationKeepsBinding() {
        String refreshToken = jwtUtils.generateRefreshToken(USER_ID, "alice", "device-1", TENANT_A, TENANT_A_CODE);

        assertEquals(TENANT_A, jwtUtils.getTenantIdFromToken(refreshToken));
        assertEquals(TENANT_A_CODE, jwtUtils.getTenantCodeFromToken(refreshToken));
    }

    /**
     * 模拟升级前签发的 Token：不带 tenantId 声明，但签名合法。
     */
    private String legacyTokenWithoutTenantClaim() throws Exception {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", USER_ID.toString());
        claims.put("username", "alice");
        claims.put("tokenType", "access");
        claims.put("jti", UUID.randomUUID().toString());

        Method createToken = JwtUtils.class.getDeclaredMethod("createToken", Map.class, String.class, long.class);
        createToken.setAccessible(true);
        return (String) createToken.invoke(jwtUtils, claims, USER_ID.toString(), 7200000L);
    }
}
