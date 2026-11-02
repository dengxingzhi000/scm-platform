package com.scmcloud.supplier.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.supplier.domain.entity.SupSupplierCertificate;
import com.scmcloud.supplier.service.ISupSupplierCertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/supplier-certificates")
@Tag(name = "供应商资质到期预警", description = "跨供应商查询即将到期或已进入预警窗口的资质证照。")
public class SupSupplierCertificateExpiryController {

    private final ISupSupplierCertificateService certificateService;

    @Operation(summary = "查询即将到期的资质", description = "按到期日升序返回未来 N 天内到期的资质证照。")
    @GetMapping("/expiring")
    public ApiResponse<Page<SupSupplierCertificate>> expiring(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "预警天数") @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(certificateService.pageExpiring(page, size, days));
    }
}
