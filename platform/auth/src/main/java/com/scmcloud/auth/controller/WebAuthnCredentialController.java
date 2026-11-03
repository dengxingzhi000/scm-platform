package com.scmcloud.auth.controller;

import com.scmcloud.auth.domain.dto.WebauthnCredentialDTO;
import com.scmcloud.auth.service.IWebauthnCredentialService;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.web.domain.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "WebAuthn 凭证", description = "提供 WebAuthn 凭证的查询、重命名、停用、删除与健康检查能力。")
public class WebAuthnCredentialController {

    private final IWebauthnCredentialService credentialService;

    @Operation(summary = "查询凭证列表", description = "获取当前用户所有处于启用状态的 WebAuthn 凭证。")
    @GetMapping("")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<WebauthnCredentialDTO>> listCredentials(
            @AuthenticationPrincipal SecurityUser user) {

        UUID userId = user.getUserId();

        log.debug("User {} listing credentials", userId);

        List<WebauthnCredentialDTO> credentials = credentialService.listActiveCredentials(userId);

        return ApiResponse.success(credentials);
    }

    @Operation(summary = "修改凭证设备名", description = "为指定凭证设置便于识别的设备名称。")
    @PutMapping("/{credentialId}/name")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebauthnCredentialDTO> updateDeviceName(
            @Parameter(description = "WebAuthn 凭证 ID") @PathVariable String credentialId,
            @NotBlank(message = "Device name cannot be empty")
            @Parameter(description = "新的设备名称") @RequestParam String deviceName,
            @AuthenticationPrincipal SecurityUser user) {

        UUID userId = user.getUserId();

        log.info("User {} updating device name for credential {}", (userId + "").replace("\r", "_").replace("\n", "_"), (credentialId + "").replace("\r", "_").replace("\n", "_"));

        WebauthnCredentialDTO credential = credentialService.updateDeviceName(
                userId, credentialId, deviceName);

        return ApiResponse.success(credential);
    }

    @Operation(summary = "删除凭证", description = "删除当前用户名下指定的 WebAuthn 凭证。")
    @DeleteMapping("/{credentialId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteCredential(
            @Parameter(description = "WebAuthn 凭证 ID") @PathVariable String credentialId,
            @AuthenticationPrincipal SecurityUser user) {

        UUID userId = user.getUserId();

        log.info("User {} deleting credential {}", (userId + "").replace("\r", "_").replace("\n", "_"), (credentialId + "").replace("\r", "_").replace("\n", "_"));

        credentialService.deleteCredential(userId, credentialId);

        return ApiResponse.success();
    }

    @Operation(summary = "停用凭证", description = "停用指定凭证，停用后将无法用于登录认证。")
    @PutMapping("/{credentialId}/deactivate")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deactivateCredential(
            @Parameter(description = "WebAuthn 凭证 ID") @PathVariable String credentialId,
            @AuthenticationPrincipal SecurityUser user) {

        UUID userId = user.getUserId();

        log.info("User {} deactivating credential {}", (userId + "").replace("\r", "_").replace("\n", "_"), (credentialId + "").replace("\r", "_").replace("\n", "_"));

        credentialService.deactivateCredential(userId, credentialId);

        return ApiResponse.success();
    }

    @Operation(summary = "检查凭证健康状态", description = "检测当前用户凭证的使用状态并返回需要处理的异常凭证。")
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