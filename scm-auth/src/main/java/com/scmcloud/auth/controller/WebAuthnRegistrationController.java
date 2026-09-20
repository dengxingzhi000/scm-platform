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
public class WebAuthnRegistrationController {

    private final IWebauthnCredentialService credentialService;
    private final WebAuthnConfig webAuthnConfig;
    private final HttpServletRequestUtils httpServletRequestUtils;

    @PostMapping("/challenge")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebAuthnRegisterChallengeResponse> generateRegistrationChallenge(
            @AuthenticationPrincipal SecurityUser user,
            HttpServletRequest request) {

        WebAuthnRequestContext ctx = WebAuthnRequestContext.of(user, request, httpServletRequestUtils);
        log.info("User {} requesting registration challenge from {}", ctx.userId(), ctx.ipAddress());

        WebAuthnRegisterChallengeResponse challenge = credentialService.generateRegistrationChallenge(
                ctx.userId(), ctx.username(), ctx.deviceId(), webAuthnConfig.getId());

        return ApiResponse.success(challenge);
    }

    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebauthnCredentialDTO> registerCredential(
            @Valid @RequestBody WebauthnRegistrationRequest request,
            @AuthenticationPrincipal SecurityUser user,
            HttpServletRequest httpRequest) {

        WebAuthnRequestContext ctx = WebAuthnRequestContext.of(user, httpRequest, httpServletRequestUtils);
        log.info("User {} registering new WebAuthn credential from {}", ctx.userId(), ctx.ipAddress());

        WebauthnCredentialDTO credential = credentialService.registerCredential(ctx.userId(), request);

        return ApiResponse.success(credential);
    }
}
