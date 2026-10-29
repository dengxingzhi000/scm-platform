package com.scmcloud.auth.controller;

import com.scmcloud.auth.domain.dto.WebauthnAuthenticationRequest;
import com.scmcloud.auth.service.IWebauthnCredentialService;
import com.scmcloud.auth.webauthn.WebAuthnConfig;
import com.scmcloud.auth.webauthn.WebAuthnRequestContext;
import com.scmcloud.common.dto.auth.TokenUpgradeResponse;
import com.scmcloud.common.dto.auth.WebAuthnChallengeResponse;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.security.util.HttpServletRequestUtils;
import com.scmcloud.common.web.domain.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WebAuthn assertion (authentication) flow.
 * <p>
 * Hosts the two endpoints that together authenticate an already-registered passkey and upgrade
 * the user's session token. The {@code /verify} handler records both successful and failed
 * authentication attempts via {@link IWebauthnCredentialService#logAuthenticationAttempt}.
 *
 * @author system
 * @since 2026-09-20
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth/webauthn/authenticate")
@RequiredArgsConstructor
@Tag(name = "WebAuthn 认证", description = "提供基于通行密钥的 WebAuthn 断言认证与令牌升级能力。")
public class WebAuthnAuthenticationController {

    private final IWebauthnCredentialService credentialService;
    private final WebAuthnConfig webAuthnConfig;
    private final HttpServletRequestUtils httpServletRequestUtils;

    @Operation(summary = "生成认证挑战", description = "为当前用户生成 WebAuthn 断言认证所需的随机挑战值。")
    @PostMapping("/challenge")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebAuthnChallengeResponse> generateAuthenticationChallenge(
            @AuthenticationPrincipal SecurityUser user,
            HttpServletRequest request) {

        WebAuthnRequestContext ctx = WebAuthnRequestContext.of(user, request, httpServletRequestUtils);
        log.info("User {} requesting authentication challenge from {}", (ctx.userId() + "").replace("\r", "_").replace("\n", "_"), (ctx.ipAddress() + "").replace("\r", "_").replace("\n", "_"));

        WebAuthnChallengeResponse challenge = credentialService.generateAuthenticationChallenge(
                ctx.userId(), ctx.deviceId(), webAuthnConfig.getId());

        return ApiResponse.success(challenge);
    }

    @Operation(summary = "校验认证并升级令牌", description = "校验 WebAuthn 断言结果，通过后升级会话令牌并记录本次认证尝试。")
    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TokenUpgradeResponse> authenticateAndUpgradeToken(
            @Valid @RequestBody WebauthnAuthenticationRequest request,
            @AuthenticationPrincipal SecurityUser user,
            HttpServletRequest httpRequest) {

        WebAuthnRequestContext ctx = WebAuthnRequestContext.of(user, httpRequest, httpServletRequestUtils);
        log.info("User {} authenticating with WebAuthn from {}", (ctx.userId() + "").replace("\r", "_").replace("\n", "_"), (ctx.ipAddress() + "").replace("\r", "_").replace("\n", "_"));

        try {
            TokenUpgradeResponse response = credentialService.authenticateAndUpgradeToken(
                    ctx.userId(), ctx.username(), request, ctx.deviceId(), ctx.ipAddress());

            // 记录成功的认证尝试
            credentialService.logAuthenticationAttempt(
                    ctx.userId(), request.getCredentialId(), true, ctx.ipAddress(), ctx.userAgent());

            return ApiResponse.success(response);

        } catch (AuthenticationException | BusinessException e) {
            // 记录失败的认证尝试
            credentialService.logAuthenticationAttempt(
                    ctx.userId(), request.getCredentialId(), false, ctx.ipAddress(), ctx.userAgent());
            throw e;
        }
    }
}