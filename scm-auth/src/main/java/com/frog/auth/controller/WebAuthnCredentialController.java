package com.frog.auth.controller;

import com.frog.auth.domain.dto.WebauthnAuthenticationRequest;
import com.frog.auth.domain.dto.WebauthnCredentialDTO;
import com.frog.auth.domain.dto.WebauthnRegistrationRequest;
import com.frog.auth.service.IWebauthnCredentialService;
import com.frog.common.dto.auth.TokenUpgradeResponse;
import com.frog.common.dto.auth.WebAuthnChallengeResponse;
import com.frog.common.dto.auth.WebAuthnRegisterChallengeResponse;
import com.frog.common.exception.BusinessException;
import com.frog.common.response.ApiResponse;
import com.frog.common.security.util.HttpServletRequestUtils;
import com.frog.common.security.util.IpUtils;
import com.frog.common.web.domain.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * WebAuthn 凭证管理控制�?
 * <p>
 * 提供WebAuthn凭证的注册、认证和管理功能
 * 参考Google Passkey和FIDO2最佳实�?
 *
 * @author system
 * @since 2025-11-27
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth/webauthn")
@RequiredArgsConstructor
public class WebAuthnCredentialController {
    /**
     * 默认 RP ID，生产环境应通过配置文件设置
     */
    private static final String DEFAULT_RP_ID = "localhost";

    private final IWebauthnCredentialService credentialService;
    private final HttpServletRequestUtils httpServletRequestUtils;

    @PostMapping("/register/challenge")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebAuthnRegisterChallengeResponse> generateRegistrationChallenge(
            @RequestParam(required = false, defaultValue = DEFAULT_RP_ID) String rpId,
            @AuthenticationPrincipal SecurityUser user,
            HttpServletRequest request) {

        UUID userId = user.getUserId();
        String username = user.getUsername();
        String deviceId = httpServletRequestUtils.getDeviceId(request);
        String ipAddress = IpUtils.getClientIp(request);

        log.info("User {} requesting registration challenge from {}", userId, ipAddress);

        WebAuthnRegisterChallengeResponse challenge = credentialService.generateRegistrationChallenge(
                userId, username, deviceId, rpId);

        return ApiResponse.success(challenge);
    }

    @PostMapping("/register/verify")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebauthnCredentialDTO> registerCredential(
            @Valid @RequestBody WebauthnRegistrationRequest request,
            @AuthenticationPrincipal SecurityUser user,
            HttpServletRequest httpRequest) {

        UUID userId = user.getUserId();
        String ipAddress = IpUtils.getClientIp(httpRequest);

        log.info("User {} registering new WebAuthn credential from {}", userId, ipAddress);

        WebauthnCredentialDTO credential = credentialService.registerCredential(userId, request);

        return ApiResponse.success(credential);
    }

    @PostMapping("/authenticate/challenge")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebAuthnChallengeResponse> generateAuthenticationChallenge(
            @RequestParam(required = false, defaultValue = DEFAULT_RP_ID) String rpId,
            @AuthenticationPrincipal SecurityUser user,
            HttpServletRequest request) {

        UUID userId = user.getUserId();
        String username = user.getUsername();
        String deviceId = httpServletRequestUtils.getDeviceId(request);
        String ipAddress = IpUtils.getClientIp(request);

        log.info("User {} requesting authentication challenge from {}", userId, ipAddress);

        WebAuthnChallengeResponse challenge = credentialService.generateAuthenticationChallenge(
                userId, username, deviceId, rpId);

        return ApiResponse.success(challenge);
    }

    @PostMapping("/authenticate/verify")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TokenUpgradeResponse> authenticateAndUpgradeToken(
            @Valid @RequestBody WebauthnAuthenticationRequest request,
            @AuthenticationPrincipal SecurityUser user,
            HttpServletRequest httpRequest) {

        UUID userId = user.getUserId();
        String username = user.getUsername();
        String deviceId = httpServletRequestUtils.getDeviceId(httpRequest);
        String ipAddress = IpUtils.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("User {} authenticating with WebAuthn from {}", userId, ipAddress);

        try {
            TokenUpgradeResponse response = credentialService.authenticateAndUpgradeToken(
                    userId, username, request, deviceId, ipAddress);

            // 记录成功的认证尝�?
            credentialService.logAuthenticationAttempt(
                    userId, request.getCredentialId(), true, ipAddress, userAgent);

            return ApiResponse.success(response);

        } catch (AuthenticationException | BusinessException e) {
            // 记录失败的认证尝�?
            credentialService.logAuthenticationAttempt(
                    userId, request.getCredentialId(), false, ipAddress, userAgent);
            throw e;
        }
    }

    @GetMapping("/credentials")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<WebauthnCredentialDTO>> listCredentials(
            @AuthenticationPrincipal SecurityUser user) {

        UUID userId = user.getUserId();

        log.debug("User {} listing credentials", userId);

        List<WebauthnCredentialDTO> credentials = credentialService.listActiveCredentials(userId);

        return ApiResponse.success(credentials);
    }

    @PutMapping("/credentials/{credentialId}/name")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebauthnCredentialDTO> updateDeviceName(
            @PathVariable String credentialId,
            @NotBlank(message = "设备名称不能为空")
            @RequestParam String deviceName,
            @AuthenticationPrincipal SecurityUser user) {

        UUID userId = user.getUserId();

        log.info("User {} updating device name for credential {}", userId, credentialId);

        WebauthnCredentialDTO credential = credentialService.updateDeviceName(
                userId, credentialId, deviceName);

        return ApiResponse.success(credential);
    }

    @DeleteMapping("/credentials/{credentialId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteCredential(
            @PathVariable String credentialId,
            @AuthenticationPrincipal SecurityUser user) {

        UUID userId = user.getUserId();

        log.info("User {} deleting credential {}", userId, credentialId);

        credentialService.deleteCredential(userId, credentialId);

        return ApiResponse.success();
    }

    @PutMapping("/credentials/{credentialId}/deactivate")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deactivateCredential(
            @PathVariable String credentialId,
            @AuthenticationPrincipal SecurityUser user) {

        UUID userId = user.getUserId();

        log.info("User {} deactivating credential {}", userId, credentialId);

        credentialService.deactivateCredential(userId, credentialId);

        return ApiResponse.success();
    }

    @GetMapping("/credentials/health")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<WebauthnCredentialDTO>> checkCredentialHealth(
            @AuthenticationPrincipal SecurityUser user) {

        UUID userId = user.getUserId();

        log.debug("User {} checking credential health", userId);

        List<WebauthnCredentialDTO> unhealthyCredentials = credentialService.checkCredentialHealth(userId);

        return ApiResponse.success(unhealthyCredentials);
    }
}