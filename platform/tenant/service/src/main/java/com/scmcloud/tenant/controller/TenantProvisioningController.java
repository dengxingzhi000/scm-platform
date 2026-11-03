package com.scmcloud.tenant.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.tenant.api.command.TenantCreateCommand;
import com.scmcloud.tenant.service.provisioning.TenantProvisioningResult;
import com.scmcloud.tenant.service.provisioning.TenantProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/provisioning")
@RequiredArgsConstructor
@Tag(name = "租户开通", description = "提供一键开通租户的编排接口，自动创建租户、试用订阅、默认配额与配置。")
public class TenantProvisioningController {

    private final TenantProvisioningService provisioningService;

    @Operation(summary = "开通租户", description = "根据入驻信息一次性完成租户及其默认订阅、资源配额的开通流程。")
    @PostMapping
    public ApiResponse<TenantProvisioningResult> provisionTenant(@Valid @RequestBody TenantCreateCommand command) {
        TenantProvisioningResult result = provisioningService.provision(command);
        if (result.isSuccess()) {
            return ApiResponse.success(result);
        } else {
            return ApiResponse.fail(500, result.getErrorMessage());
        }
    }
}
