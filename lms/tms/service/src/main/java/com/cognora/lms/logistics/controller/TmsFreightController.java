package com.cognora.lms.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.lms.logistics.domain.dto.FreightQuoteRequest;
import com.cognora.lms.logistics.domain.dto.FreightReconcileRequest;
import com.cognora.lms.logistics.domain.entity.TmsFreightReconciliation;
import com.cognora.lms.logistics.domain.entity.TmsFreightTemplate;
import com.cognora.lms.logistics.domain.freight.FreightQuote;
import com.cognora.lms.logistics.service.ITmsFreightReconciliationService;
import com.cognora.lms.logistics.service.ITmsFreightTemplateService;
import com.cognora.lms.logistics.support.FreightPermissionGuard;
import com.cognora.platform.common.log.annotation.AuditLog;
import com.cognora.platform.common.response.ApiResponse;
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

/**
 * 运费模板、运费试算与对账接口（批次 2）。
 */
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/tms-freight")
@Tag(name = "运费与对账", description = "运费模板、运费试算与承运商账单差异对账。")
public class TmsFreightController {

    private final ITmsFreightTemplateService templateService;
    private final ITmsFreightReconciliationService reconciliationService;
    private final FreightPermissionGuard freightPermissionGuard;

    @Operation(summary = "分页查询运费模板")
    @GetMapping("/template/list")
    public ApiResponse<Page<TmsFreightTemplate>> pageTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "承运商主键 ID") @RequestParam(required = false) String carrierId,
            @Parameter(description = "区域编码，如省份或 DEFAULT") @RequestParam(required = false) String regionCode) {
        return ApiResponse.success(templateService.pageList(page, size, carrierId, regionCode));
    }

    @Operation(summary = "按承运商查询运费模板")
    @GetMapping("/template/carrier/{carrierId}")
    public ApiResponse<List<TmsFreightTemplate>> listByCarrier(@PathVariable String carrierId) {
        return ApiResponse.success(templateService.listByCarrier(carrierId));
    }

    @Operation(summary = "创建运费模板")
    @PostMapping("/template")
    @AuditLog(operation = "创建运费模板", businessType = "LOGISTICS", riskLevel = 2)
    public ApiResponse<TmsFreightTemplate> createTemplate(@RequestBody TmsFreightTemplate template) {
        return ApiResponse.success(templateService.createTemplate(template));
    }

    @Operation(summary = "更新运费模板")
    @PutMapping("/template/{id}")
    @AuditLog(operation = "更新运费模板", businessType = "LOGISTICS", riskLevel = 2)
    public ApiResponse<TmsFreightTemplate> updateTemplate(
            @PathVariable String id, @RequestBody TmsFreightTemplate template) {
        return ApiResponse.success(templateService.updateTemplate(id, template));
    }

    @Operation(summary = "删除运费模板")
    @DeleteMapping("/template/{id}")
    @AuditLog(operation = "删除运费模板", businessType = "LOGISTICS", riskLevel = 3)
    public ApiResponse<Void> deleteTemplate(@PathVariable String id) {
        templateService.deleteTemplate(id);
        return ApiResponse.success();
    }

    @Operation(summary = "运费试算", description = "按承运商 + 区域解析模板，计算首重/续重/体积重与区域价后的应收运费。")
    @PostMapping("/quote")
    public ApiResponse<FreightQuote> quote(@RequestBody FreightQuoteRequest request) {
        return ApiResponse.success(templateService.quote(
                request.carrierId(), request.regionCode(), request.weight(), request.volume()));
    }

    @Operation(summary = "运费对账", description = "按模板重算应收，与承运商账单比较并落差异分类；同批次同运单幂等。")
    @PostMapping("/reconciliation")
    @AuditLog(operation = "运费对账", businessType = "LOGISTICS", riskLevel = 3)
    public ApiResponse<TmsFreightReconciliation> reconcile(@RequestBody FreightReconcileRequest request) {
        return ApiResponse.success(reconciliationService.reconcile(
                request.waybillId(), request.regionCode(), request.billedAmount(),
                request.batchNo(), request.operator()));
    }

    @Operation(summary = "确认对账差异", description = "OPEN → CONFIRMED；需要权限码 lms:freight:reconciliation:confirm。")
    @PostMapping("/reconciliation/{id}/confirm")
    @AuditLog(operation = "确认运费对账差异", businessType = "LOGISTICS", riskLevel = 3)
    public ApiResponse<TmsFreightReconciliation> confirmReconciliation(
            @PathVariable String id,
            @Parameter(description = "操作人") @RequestParam(required = false) String operator) {
        freightPermissionGuard.requireReconciliationConfirmPermission();
        return ApiResponse.success(reconciliationService.confirm(id, operator));
    }

    @Operation(summary = "分页查询对账差异")
    @GetMapping("/reconciliation/list")
    public ApiResponse<Page<TmsFreightReconciliation>> pageReconciliations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "对账批次号") @RequestParam(required = false) String batchNo,
            @Parameter(description = "差异类型 MATCH / OVERCHARGE / UNDERCHARGE") @RequestParam(required = false) String diffType,
            @Parameter(description = "处理状态 OPEN / CONFIRMED") @RequestParam(required = false) String status) {
        return ApiResponse.success(reconciliationService.pageList(page, size, batchNo, diffType, status));
    }
}
