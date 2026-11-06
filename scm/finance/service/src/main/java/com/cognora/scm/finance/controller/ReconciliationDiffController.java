package com.cognora.scm.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.log.annotation.AuditLog;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.scm.finance.domain.entity.ReconciliationDiff;
import com.cognora.scm.finance.service.ReconciliationDiffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/reconciliation-diff")
@Tag(name = "对账差异", description = "对账差异的发现、确认、驳回与核销闭环。")
public class ReconciliationDiffController {

    private final ReconciliationDiffService reconciliationDiffService;

    @Operation(summary = "登记对账差异", description = "在对账记录下登记差异，初始状态为待处理。")
    @PostMapping
    @AuditLog(operation = "登记对账差异", businessType = "FINANCE_RECONCILIATION_DIFF", riskLevel = 3)
    public ApiResponse<ReconciliationDiff> create(@RequestBody ReconciliationDiff diff) {
        return ApiResponse.success(reconciliationDiffService.create(diff));
    }

    @Operation(summary = "确认差异", description = "待处理 → 已确认；只有已确认差异才能核销。")
    @PostMapping("/{id}/confirm")
    @AuditLog(operation = "确认对账差异", businessType = "FINANCE_RECONCILIATION_DIFF", riskLevel = 3)
    public ApiResponse<ReconciliationDiff> confirm(
            @Parameter(description = "差异 ID") @PathVariable String id,
            @Parameter(description = "处理说明") @RequestParam String resolution,
            @Parameter(description = "处理人 ID") @RequestParam(required = false) String resolvedBy) {
        return ApiResponse.success(reconciliationDiffService.confirm(id, resolution, resolvedBy));
    }

    @Operation(summary = "驳回差异", description = "待处理 → 已驳回（终态）。")
    @PostMapping("/{id}/reject")
    @AuditLog(operation = "驳回对账差异", businessType = "FINANCE_RECONCILIATION_DIFF", riskLevel = 3)
    public ApiResponse<ReconciliationDiff> reject(
            @Parameter(description = "差异 ID") @PathVariable String id,
            @Parameter(description = "驳回原因") @RequestParam String reason,
            @Parameter(description = "处理人 ID") @RequestParam(required = false) String resolvedBy) {
        return ApiResponse.success(reconciliationDiffService.reject(id, reason, resolvedBy));
    }

    @Operation(summary = "核销差异", description = "已确认 → 已核销（终态）；非法流转返回 409。")
    @PostMapping("/{id}/write-off")
    @AuditLog(operation = "核销对账差异", businessType = "FINANCE_RECONCILIATION_DIFF", riskLevel = 4)
    public ApiResponse<ReconciliationDiff> writeOff(
            @Parameter(description = "差异 ID") @PathVariable String id,
            @Parameter(description = "核销说明") @RequestParam String resolution,
            @Parameter(description = "核销人 ID") @RequestParam(required = false) String resolvedBy) {
        return ApiResponse.success(reconciliationDiffService.writeOff(id, resolution, resolvedBy));
    }

    @Operation(summary = "查询差异详情", description = "根据差异 ID 查询差异状态与处理记录。")
    @GetMapping("/{id}")
    public ApiResponse<ReconciliationDiff> getById(@Parameter(description = "差异 ID") @PathVariable String id) {
        return ApiResponse.success(reconciliationDiffService.getById(id));
    }

    @Operation(summary = "分页查询差异", description = "按对账记录与状态分页查询差异。")
    @GetMapping("/page")
    public ApiResponse<Page<ReconciliationDiff>> page(
            @Parameter(description = "对账记录 ID") @RequestParam(required = false) String reconciliationId,
            @Parameter(description = "差异状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量，默认 20") @RequestParam(required = false) Integer size) {
        return ApiResponse.success(reconciliationDiffService.pageQuery(
                reconciliationId, status, page == null ? 1 : page, size == null ? 20 : size));
    }
}
