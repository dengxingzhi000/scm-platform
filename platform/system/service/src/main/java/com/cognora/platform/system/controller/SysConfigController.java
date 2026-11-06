package com.cognora.platform.system.controller;

import com.cognora.platform.common.dto.config.SysConfigItemDTO;
import com.cognora.platform.common.log.annotation.AuditLog;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.system.service.ISysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台/租户系统配置控制器。
 *
 * @author Deng
 * @since 2026-11-06
 */
@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "平台级默认配置与租户覆盖配置的查询与修改。")
public class SysConfigController {

    private final ISysConfigService configService;

    @Operation(summary = "查询系统配置", description = "返回平台级默认值与当前租户覆盖的合并视图，每项标明来源。")
    @GetMapping
    @PreAuthorize("hasAuthority('system:config:view')")
    public ApiResponse<List<SysConfigItemDTO>> getConfig() {
        return ApiResponse.success(configService.getMergedConfig());
    }

    @Operation(summary = "修改系统配置", description = "租户管理员只能修改本租户覆盖；平台级配置仅平台超级管理员可修改。")
    @PutMapping
    @PreAuthorize("hasAuthority('system:config:edit')")
    @AuditLog(
            operation = "修改系统配置",
            businessType = "CONFIG",
            riskLevel = 3
    )
    public ApiResponse<SysConfigItemDTO> updateConfig(
            @Validated @RequestBody SysConfigItemDTO request) {
        return ApiResponse.success(configService.updateConfig(request));
    }
}
