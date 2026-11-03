package com.scmcloud.auth.controller;

import com.scmcloud.auth.domain.dto.WebauthnCredentialDTO;
import com.scmcloud.auth.domain.dto.WebauthnRegistrationRequest;
import com.scmcloud.auth.service.IWebauthnCredentialService;
import com.scmcloud.auth.webauthn.WebAuthnConfig;
import com.scmcloud.auth.webauthn.WebAuthnRequestContext;
import com.scmcloud.common.dto.auth.WebAuthnRegisterChallengeResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WebAuthn credential registration flow.
 * <p>
 * Hosts the two endpoints that together register a new passkey for an authenticated user:
 * challenge generation and credential verification.
 *
 * @author system
 * @since 2026-09-20
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth/webauthn/register")
@RequiredArgsConstructor
@Tag(name = "WebAuthn 注册", description = "提供 WebAuthn 通行密钥的注册挑战生成与凭证登记能力。")
public class WebAuthnRegistrationController {

    private final IWebauthnCredentialService credentialService;
    private final WebAuthnConfig webAuthnConfig;
    private final HttpServletRequestUtils httpServletRequestUtils;

    @Operation(summary = "生成注册挑战", description = "为当前用户生成 WebAuthn 凭证注册所需的随机挑战值。")
    @PostMapping("/challenge")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebAuthnRegisterChallengeResponse> generateRegistrationChallenge(
            @AuthenticationPrincipal SecurityUser user,
            HttpServletRequest request) {

        WebAuthnRequestContext ctx = WebAuthnRequestContext.of(user, request, httpServletRequestUtils);
        log.info("User {} requesting registration challenge from {}", (ctx.userId() + "").replace("\r", "_").replace("\n", "_"), (ctx.ipAddress() + "").replace("\r", "_").replace("\n", "_"));

        WebAuthnRegisterChallengeResponse challenge = credentialService.generateRegistrationChallenge(
                ctx.userId(), ctx.username(), ctx.deviceId(), webAuthnConfig.getId());

        return ApiResponse.success(challenge);
    }

    @Operation(summary = "校验注册并保存凭证", description = "校验 WebAuthn 注册结果并保存新的通行密钥凭证。")
    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebauthnCredentialDTO> registerCredential(
            @Valid @RequestBody WebauthnRegistrationRequest request,
            @AuthenticationPrincipal SecurityUser user,
            HttpServletRequest httpRequest) {

        WebAuthnRequestContext ctx = WebAuthnRequestContext.of(user, httpRequest, httpServletRequestUtils);
        log.info("User {} registering new WebAuthn credential from {}", (ctx.userId() + "").replace("\r", "_").replace("\n", "_"), (ctx.ipAddress() + "").replace("\r", "_").replace("\n", "_"));

        WebauthnCredentialDTO credential = credentialService.registerCredential(ctx.userId(), request);

        return ApiResponse.success(credential);
    }
}
