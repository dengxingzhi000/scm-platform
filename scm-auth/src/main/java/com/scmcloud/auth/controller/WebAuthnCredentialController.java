package com.scmcloud.auth.controller;

import com.scmcloud.auth.domain.dto.WebauthnCredentialDTO;
import com.scmcloud.auth.service.IWebauthnCredentialService;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.web.domain.SecurityUser;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * WebAuthn 鍑瘉绠$悊鎺у埗锟?
 * <p>
 * 鎻愪緵WebAuthn鍑瘉鐨勬敞鍐屻€佽璇佸拰绠$悊鍔熻兘
 * 鍙傝€僄oogle Passkey鍜孎IDO2鏈€浣冲疄锟?
 *
 * @author system
 * @since 2025-11-27
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth/webauthn/credentials")
@RequiredArgsConstructor
public class WebAuthnCredentialController {

    private final IWebauthnCredentialService credentialService;

    @GetMapping("")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<WebauthnCredentialDTO>> listCredentials(
            @AuthenticationPrincipal SecurityUser user) {

        UUID userId = user.getUserId();

        log.debug("User {} listing credentials", userId);

        List<WebauthnCredentialDTO> credentials = credentialService.listActiveCredentials(userId);

        return ApiResponse.success(credentials);
    }

    @PutMapping("/{credentialId}/name")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebauthnCredentialDTO> updateDeviceName(
            @PathVariable String credentialId,
            @NotBlank(message = "Device name cannot be empty")
            @RequestParam String deviceName,
            @AuthenticationPrincipal SecurityUser user) {

        UUID userId = user.getUserId();

        log.info("User {} updating device name for credential {}", userId, credentialId);

        WebauthnCredentialDTO credential = credentialService.updateDeviceName(
                userId, credentialId, deviceName);

        return ApiResponse.success(credential);
    }

    @DeleteMapping("/{credentialId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteCredential(
            @PathVariable String credentialId,
            @AuthenticationPrincipal SecurityUser user) {

        UUID userId = user.getUserId();

        log.info("User {} deleting credential {}", userId, credentialId);

        credentialService.deleteCredential(userId, credentialId);

        return ApiResponse.success();
    }

    @PutMapping("/{credentialId}/deactivate")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deactivateCredential(
            @PathVariable String credentialId,
            @AuthenticationPrincipal SecurityUser user) {

        UUID userId = user.getUserId();

        log.info("User {} deactivating credential {}", userId, credentialId);

        credentialService.deactivateCredential(userId, credentialId);

        return ApiResponse.success();
    }

    @GetMapping("/health")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<WebauthnCredentialDTO>> checkCredentialHealth(
            @AuthenticationPrincipal SecurityUser user) {

        UUID userId = user.getUserId();

        log.debug("User {} checking credential health", userId);

        List<WebauthnCredentialDTO> unhealthyCredentials = credentialService.checkCredentialHealth(userId);

        return ApiResponse.success(unhealthyCredentials);
    }
}