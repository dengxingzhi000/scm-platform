package com.cognora.platform.auth.controller;

import com.cognora.platform.auth.service.TwoFactorService;
import com.cognora.platform.common.dto.user.RecoveryCodesResponse;
import com.cognora.platform.common.dto.user.TwoFactorSetupResponse;
import com.cognora.platform.common.dto.user.TwoFactorVerifyRequest;
import com.cognora.platform.common.log.annotation.AuditLog;
import com.cognora.platform.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2FA 自助管理。目标用户只从访问令牌解析，不接受 userId 入参。
 */
@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
@Tag(name = "二次验证", description = "TOTP 初始化、启用、关闭与恢复码重生成。")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    @Operation(summary = "初始化二次验证", description = "返回秘钥与 otpauth URI，秘钥 10 分钟内有效。")
    @PostMapping("/setup")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TwoFactorSetupResponse> setup() {
        return ApiResponse.success(twoFactorService.setup());
    }

    @Operation(summary = "启用二次验证", description = "校验 TOTP 后持久化秘钥。")
    @PostMapping("/enable")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(operation = "启用二次验证", businessType = "USER", riskLevel = 3)
    public ApiResponse<Void> enable(@Valid @RequestBody TwoFactorVerifyRequest request) {
        twoFactorService.enable(request);
        return ApiResponse.success();
    }

    @Operation(summary = "关闭二次验证", description = "校验 TOTP 或恢复码后关闭。")
    @PostMapping("/disable")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(operation = "关闭二次验证", businessType = "USER", riskLevel = 3)
    public ApiResponse<Void> disable(@Valid @RequestBody TwoFactorVerifyRequest request) {
        twoFactorService.disable(request);
        return ApiResponse.success();
    }

    @Operation(summary = "重生成恢复码", description = "校验 TOTP 后返回 10 个一次性恢复码，仅展示一次。")
    @PostMapping("/recovery-codes/regenerate")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(operation = "重生成恢复码", businessType = "USER", riskLevel = 3)
    public ApiResponse<RecoveryCodesResponse> regenerateRecoveryCodes(
            @Valid @RequestBody TwoFactorVerifyRequest request) {
        return ApiResponse.success(twoFactorService.regenerateRecoveryCodes(request));
    }
}
