package com.scmcloud.supplier.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.supplier.domain.entity.SupSupplier;
import com.scmcloud.supplier.domain.entity.SupSupplierCategory;
import com.scmcloud.supplier.domain.entity.SupSupplierChangeLog;
import com.scmcloud.supplier.dto.SupplierAuditRequest;
import com.scmcloud.supplier.dto.SupplierPerformanceVO;
import com.scmcloud.supplier.dto.SupplierProfileVO;
import com.scmcloud.supplier.dto.SupplierStatsVO;
import com.scmcloud.supplier.dto.SupplierUpsertRequest;
import com.scmcloud.supplier.service.ISupSupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/suppliers")
@Tag(name = "供应商管理", description = "供应商主数据、准入审核、黑名单、分类、绩效与变更留痕。")
public class SupSupplierController {

    private final ISupSupplierService supplierService;

    @Operation(summary = "查询供应商详情", description = "根据供应商ID查询未删除的供应商详情。")
    @GetMapping("/{id}")
    public ApiResponse<SupSupplier> getById(@Parameter(description = "供应商ID") @PathVariable String id) {
        log.info("[API] 查询供应商详情 id={}", id);
        SupSupplier supplier = supplierService.getById(id);
        if (supplier == null || Boolean.TRUE.equals(supplier.getDeleted())) {
            return ApiResponse.fail(404, "供应商不存在");
        }
        return ApiResponse.success(supplier);
    }

    @Operation(summary = "查询供应商画像", description = "返回主数据、联系人、银行账户、资质、分类与最近变更日志。")
    @GetMapping("/{id}/profile")
    public ApiResponse<SupplierProfileVO> getProfile(@Parameter(description = "供应商ID") @PathVariable String id) {
        log.info("[API] 查询供应商画像 id={}", id);
        return ApiResponse.success(supplierService.getProfile(id));
    }

    @Operation(summary = "查询供应商绩效评分卡", description = "按质量/交付/服务/价格维度汇总评价得分并给出分级建议。")
    @GetMapping("/{id}/performance")
    public ApiResponse<SupplierPerformanceVO> getPerformance(
            @Parameter(description = "供应商ID") @PathVariable String id) {
        log.info("[API] 查询供应商绩效 id={}", id);
        return ApiResponse.success(supplierService.getPerformance(id));
    }

    @Operation(summary = "查询供应商变更日志", description = "按时间倒序返回供应商主数据、准入与黑名单变更记录。")
    @GetMapping("/{id}/change-logs")
    public ApiResponse<List<SupSupplierChangeLog>> listChangeLogs(
            @Parameter(description = "供应商ID") @PathVariable String id,
            @RequestParam(defaultValue = "20") int limit) {
        log.info("[API] 查询供应商变更日志 id={}, limit={}", id, limit);
        return ApiResponse.success(supplierService.listChangeLogs(id, limit));
    }

    @Operation(summary = "查询供应商分类", description = "查询指定供应商已关联的分类。")
    @GetMapping("/{id}/categories")
    public ApiResponse<List<SupSupplierCategory>> getCategories(
            @Parameter(description = "供应商ID") @PathVariable String id) {
        return ApiResponse.success(supplierService.getCategories(id));
    }

    @Operation(summary = "设置供应商分类", description = "全量替换指定供应商的分类关联。")
    @PutMapping("/{id}/categories")
    public ApiResponse<Void> assignCategories(
            @Parameter(description = "供应商ID") @PathVariable String id,
            @RequestBody List<String> categoryIds,
            @RequestParam(required = false) String operatorId,
            @RequestParam(required = false) String operatorName) {
        supplierService.assignCategories(id, categoryIds, operatorId, operatorName);
        return ApiResponse.success();
    }

    @Operation(summary = "查询供应商统计", description = "返回供应商总数、启用数、合作中、待审核、黑名单与资质到期数量。")
    @GetMapping("/stats")
    public ApiResponse<SupplierStatsVO> stats() {
        return ApiResponse.success(supplierService.getStats());
    }

    @Operation(summary = "创建供应商", description = "新增供应商主数据，初始化为草稿准入状态并自动生成编码。")
    @PostMapping
    public ApiResponse<SupSupplier> create(@Valid @RequestBody SupplierUpsertRequest request) {
        log.info("[API] 创建供应商 name={}", request.getSupplierName());
        return ApiResponse.success(supplierService.createSupplier(request));
    }

    @Operation(summary = "更新供应商", description = "更新供应商主数据；准入状态、黑名单与评分由流程维护。")
    @PutMapping("/{id}")
    public ApiResponse<SupSupplier> update(@Parameter(description = "供应商ID") @PathVariable String id,
                                           @Valid @RequestBody SupplierUpsertRequest request) {
        log.info("[API] 更新供应商 id={}", id);
        return ApiResponse.success(supplierService.updateSupplier(id, request));
    }

    @Operation(summary = "删除供应商", description = "逻辑删除供应商；合作中的供应商需先终止合作。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "供应商ID") @PathVariable String id,
                                    @RequestParam(required = false) String operatorId,
                                    @RequestParam(required = false) String operatorName) {
        log.info("[API] 删除供应商 id={}", id);
        supplierService.deleteSupplier(id, operatorId, operatorName);
        return ApiResponse.success();
    }

    @Operation(summary = "分页查询供应商", description = "按关键字、类型、合作/准入/分级/风险等条件分页查询供应商。")
    @GetMapping
    public ApiResponse<Page<SupSupplier>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商类型") @RequestParam(required = false) Integer supplierType,
            @Parameter(description = "合作状态") @RequestParam(required = false) Integer cooperationStatus,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "准入状态") @RequestParam(required = false) Integer auditStatus,
            @Parameter(description = "供应商分级") @RequestParam(required = false) Integer supplierLevel,
            @Parameter(description = "风险等级") @RequestParam(required = false) Integer riskLevel,
            @Parameter(description = "是否黑名单") @RequestParam(required = false) Boolean blacklisted,
            @Parameter(description = "分类ID") @RequestParam(required = false) String categoryId) {
        log.info("[API] 分页查询供应商 page={}, size={}, keyword={}", page, size, keyword);
        Page<SupSupplier> result = supplierService.pageList(page, size, keyword, supplierType,
                cooperationStatus, enabled, auditStatus, supplierLevel, riskLevel, blacklisted, categoryId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "查询可合作供应商", description = "查询启用、准入通过、非黑名单且合作中的供应商。")
    @GetMapping("/active")
    public ApiResponse<List<SupSupplier>> listActive() {
        return ApiResponse.success(supplierService.listActive());
    }

    @Operation(summary = "搜索供应商", description = "根据供应商名称关键字模糊搜索供应商。")
    @GetMapping("/search")
    public ApiResponse<List<SupSupplier>> searchByName(
            @Parameter(description = "供应商名称关键字") @RequestParam String name) {
        return ApiResponse.success(supplierService.searchByName(name));
    }

    @Operation(summary = "启用供应商", description = "仅准入审核通过且未列入黑名单的供应商可启用。")
    @PutMapping("/{id}/enable")
    public ApiResponse<Void> enable(@Parameter(description = "供应商ID") @PathVariable String id) {
        log.info("[API] 启用供应商 id={}", id);
        boolean success = supplierService.enableSupplier(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "启用失败");
    }

    @Operation(summary = "停用供应商", description = "将指定供应商状态置为停用。")
    @PutMapping("/{id}/disable")
    public ApiResponse<Void> disable(@Parameter(description = "供应商ID") @PathVariable String id) {
        log.info("[API] 停用供应商 id={}", id);
        boolean success = supplierService.disableSupplier(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "停用失败");
    }

    @Operation(summary = "提交准入审核", description = "将草稿或被驳回的供应商提交审核，要求税号已补全。")
    @PostMapping("/{id}/submit-audit")
    public ApiResponse<Void> submitAudit(@Parameter(description = "供应商ID") @PathVariable String id,
                                         @RequestParam(required = false) String operatorId,
                                         @RequestParam(required = false) String operatorName) {
        supplierService.submitAudit(id, operatorId, operatorName);
        return ApiResponse.success();
    }

    @Operation(summary = "审核供应商准入", description = "APPROVE 通过并启用供应商；REJECT 驳回并记录原因。")
    @PutMapping("/{id}/audit")
    public ApiResponse<Void> audit(@Parameter(description = "供应商ID") @PathVariable String id,
                                   @RequestBody SupplierAuditRequest request) {
        supplierService.audit(id, request);
        return ApiResponse.success();
    }

    @Operation(summary = "列入黑名单", description = "将供应商列入黑名单，同时停用并终止合作。")
    @PutMapping("/{id}/blacklist")
    public ApiResponse<Void> blacklist(@Parameter(description = "供应商ID") @PathVariable String id,
                                       @RequestParam String reason,
                                       @RequestParam(required = false) String operatorId,
                                       @RequestParam(required = false) String operatorName) {
        supplierService.blacklist(id, reason, operatorId, operatorName);
        return ApiResponse.success();
    }

    @Operation(summary = "移出黑名单", description = "将供应商移出黑名单，启用状态与准入状态保持不变。")
    @PutMapping("/{id}/unblacklist")
    public ApiResponse<Void> unblacklist(@Parameter(description = "供应商ID") @PathVariable String id,
                                         @RequestParam(required = false) String operatorId,
                                         @RequestParam(required = false) String operatorName) {
        supplierService.unblacklist(id, operatorId, operatorName);
        return ApiResponse.success();
    }

    @Operation(summary = "调整风险等级", description = "风险等级 1-低,2-中,3-高，变更写入供应商变更日志。")
    @PutMapping("/{id}/risk-level")
    public ApiResponse<Void> changeRiskLevel(
            @Parameter(description = "供应商ID") @PathVariable String id,
            @Parameter(description = "目标风险等级") @RequestParam Integer level,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String operatorId,
            @RequestParam(required = false) String operatorName) {
        supplierService.changeRiskLevel(id, level, reason, operatorId, operatorName);
        return ApiResponse.success();
    }

    @Operation(summary = "变更合作状态", description = "合作状态 0-潜在,1-合作中,2-暂停,3-终止，按合法路径流转。")
    @PutMapping("/{id}/cooperation-status")
    public ApiResponse<Void> changeCooperationStatus(
            @Parameter(description = "供应商ID") @PathVariable String id,
            @Parameter(description = "目标合作状态") @RequestParam Integer status,
            @RequestParam(required = false) String operatorId,
            @RequestParam(required = false) String operatorName) {
        supplierService.changeCooperationStatus(id, status, operatorId, operatorName);
        return ApiResponse.success();
    }
}
