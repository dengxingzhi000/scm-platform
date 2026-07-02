package com.scmcloud.auth.service;

import com.scmcloud.common.security.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * OAuth2 登出业务服务
 *
 * <p>封装 token 撤销逻辑，与 Controller 解耦
 *
 * @author Deng
 * @since 2025-11-10
 * @version 1.1
 * @apiNote 1.1 抽取 Service 层，修复职责混乱
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2LogoutService {
    private final OAuth2AuthorizationService authorizationService;
    private final JwtUtils jwtUtils;

    /**
     * 撤销指定客户端的授权
     *
     * @param accessToken access token
     * @param clientId 客户端 ID
     * @param userId 用户 ID（用于日志）
     */
    public void revokeClientAuthorization(String accessToken, String clientId, UUID userId) {
        OAuth2Authorization authorization =
                authorizationService.findByToken(accessToken, OAuth2TokenType.ACCESS_TOKEN);
        if (authorization != null) {
            authorizationService.remove(authorization);
            log.info("Revoked authorization for userId={} clientId={}", userId, clientId);
        } else {
            log.warn("Authorization not found for userId={} clientId={}", userId, clientId);
        }
    }

    /**
     * 撤销用户所有 token（全局登出）
     *
     * @param userId 用户 ID
     */
    public void revokeAllTokens(UUID userId) {
        jwtUtils.revokeAllUserTokens(userId);
        log.info("Global logout: revoked all tokens for userId={}", userId);
    }
}
