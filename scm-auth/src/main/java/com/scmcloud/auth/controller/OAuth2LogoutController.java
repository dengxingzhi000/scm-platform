package com.scmcloud.auth.controller;

import com.scmcloud.auth.service.OAuth2LogoutService;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.security.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * OAuth2 登出控制器
 *
 * <p>提供 OAuth2 授权撤销功能，业务逻辑由 {@link OAuth2LogoutService} 处理
 *
 * @author Deng
 * @since 2025-11-10
 * @version 1.1
 * @apiNote 1.1 抽取 Service 层，移除冗余校验，修复职责混乱
 */
@Slf4j
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OAuth2LogoutController {
    private static final String BEARER_PREFIX = "Bearer ";

    private final OAuth2LogoutService logoutService;
    private final JwtUtils jwtUtils;

    /**
     * 登出接口
     *
     * @param authHeader Authorization 头，格式 "Bearer {token}"
     * @param clientId   可选，指定客户端 ID 则仅撤销该客户端授权；不传则全局登出
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String clientId) {

        String accessToken = authHeader.substring(BEARER_PREFIX.length());
        UUID userId = jwtUtils.getUserIdFromToken(accessToken);

        if (StringUtils.hasText(clientId)) {
            logoutService.revokeClientAuthorization(accessToken, clientId, userId);
        } else {
            logoutService.revokeAllTokens(userId);
        }

        return ApiResponse.success();
    }
}
