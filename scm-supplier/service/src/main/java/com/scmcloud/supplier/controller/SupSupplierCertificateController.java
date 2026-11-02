package com.scmcloud.supplier.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.supplier.domain.entity.SupSupplierCertificate;
import com.scmcloud.supplier.service.ISupSupplierCertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/suppliers/{supplierId}/certificates")
@Tag(name = "供应商资质证照", description = "维护营业执照、质量体系、生产许可等资质及有效期。")
public class SupSupplierCertificateController {

    private final ISupSupplierCertificateService certificateService;

    @Operation(summary = "查询资质列表", description = "按有效期升序查询指定供应商的资质证照。")
    @GetMapping
    public ApiResponse<List<SupSupplierCertificate>> list(
            @Parameter(description = "供应商ID") @PathVariable String supplierId) {
        return ApiResponse.success(certificateService.listBySupplierId(supplierId));
    }

    @Operation(summary = "新增资质证照", description = "新增供应商资质，未指定状态时按有效期自动判定。")
    @PostMapping
    public ApiResponse<SupSupplierCertificate> create(
            @Parameter(description = "供应商ID") @PathVariable String supplierId,
            @RequestBody SupSupplierCertificate certificate) {
        log.info("[API] 新增供应商资质 supplierId={}", supplierId);
        return ApiResponse.success(certificateService.create(supplierId, certificate));
    }

    @Operation(summary = "更新资质证照", description = "更新资质证照；有效期变化时重新判定有效/临期/过期。")
    @PutMapping("/{id}")
    public ApiResponse<SupSupplierCertificate> update(
            @Parameter(description = "供应商ID") @PathVariable String supplierId,
            @Parameter(description = "资质ID") @PathVariable String id,
            @RequestBody SupSupplierCertificate certificate) {
        return ApiResponse.success(certificateService.update(id, certificate));
    }

    @Operation(summary = "审核资质证照", description = "审核通过后按有效期标记有效/临期/过期；驳回标记为已驳回。")
    @PutMapping("/{id}/verify")
    public ApiResponse<Void> verify(
            @Parameter(description = "供应商ID") @PathVariable String supplierId,
            @Parameter(description = "资质ID") @PathVariable String id,
            @RequestParam boolean approved,
            @RequestParam(required = false) String operatorId,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String remark) {
        certificateService.verify(id, approved, operatorId, operatorName, remark);
        return ApiResponse.success();
    }

    @Operation(summary = "删除资质证照", description = "逻辑删除供应商资质证照。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "供应商ID") @PathVariable String supplierId,
            @Parameter(description = "资质ID") @PathVariable String id) {
        boolean success = certificateService.delete(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "资质证照不存在");
    }
}
