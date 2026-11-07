package com.cognora.scm.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.log.annotation.AuditLog;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.scm.finance.domain.dto.CreateBadDebtWriteOffRequest;
import com.cognora.scm.finance.domain.entity.BadDebtWriteOff;
import com.cognora.scm.finance.service.BadDebtWriteOffService;
import com.cognora.scm.finance.service.query.BadDebtWriteOffQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/bad-debt-write-off")
@Tag(name = "坏账核销", description = "坏账核销单据流：申请、审批（通过/驳回）、核销生效。")
public class BadDebtWriteOffController {

    private final BadDebtWriteOffService badDebtWriteOffService;
    private final BadDebtWriteOffQueryService badDebtWriteOffQueryService;

    @Operation(summary = "分页查询核销单", description = "按核销单号/往来单位关键字、账期和状态分页查询核销单。")
    @GetMapping("/page")
    public ApiResponse<Page<BadDebtWriteOff>> page(
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量，默认 20") @RequestParam(required = false) Integer size,
            @Parameter(description = "核销单号或往来单位关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "核销账期 YYYY-MM") @RequestParam(required = false) String periodCode,
            @Parameter(description = "状态：0 申请中 / 1 已批准 / 2 已核销 / 3 已驳回") @RequestParam(required = false) Integer status) {
        return ApiResponse.success(badDebtWriteOffQueryService.pageQuery(
                page == null ? 1 : page, size == null ? 20 : size, keyword, periodCode, status));
    }

    @Operation(summary = "查询核销单详情", description = "根据核销单 ID 查询申请、审批与生效信息。")
    @GetMapping("/{id}")
    public ApiResponse<BadDebtWriteOff> getById(@Parameter(description = "核销单 ID") @PathVariable String id) {
        return ApiResponse.success(badDebtWriteOffService.getById(id));
    }

    @Operation(summary = "提交核销申请", description = "金额与账期绑定，同一结算单/发票只允许一张进行中的核销单。")
    @PostMapping
    @AuditLog(operation = "提交坏账核销申请", businessType = "FINANCE_BAD_DEBT_WRITE_OFF", riskLevel = 3)
    public ApiResponse<BadDebtWriteOff> apply(@RequestBody CreateBadDebtWriteOffRequest request) {
        return ApiResponse.success(badDebtWriteOffService.apply(request));
    }

    @Operation(summary = "审批通过核销申请", description = "申请中 → 已批准，只有已批准的核销单才能生效。")
    @PostMapping("/{id}/approve")
    @AuditLog(operation = "审批坏账核销申请", businessType = "FINANCE_BAD_DEBT_WRITE_OFF", riskLevel = 4)
    public ApiResponse<BadDebtWriteOff> approve(
            @Parameter(description = "核销单 ID") @PathVariable String id,
            @Parameter(description = "审批人 ID") @RequestParam String approverId,
            @Parameter(description = "审批人姓名") @RequestParam String approverName,
            @Parameter(description = "审批意见") @RequestParam(required = false) String remark) {
        return ApiResponse.success(badDebtWriteOffService.approve(id, approverId, approverName, remark));
    }

    @Operation(summary = "驳回核销申请", description = "申请中 → 已驳回；已批准、已核销的单据拒绝驳回。")
    @PostMapping("/{id}/reject")
    @AuditLog(operation = "驳回坏账核销申请", businessType = "FINANCE_BAD_DEBT_WRITE_OFF", riskLevel = 4)
    public ApiResponse<BadDebtWriteOff> reject(
            @Parameter(description = "核销单 ID") @PathVariable String id,
            @Parameter(description = "驳回原因") @RequestParam String reason,
            @Parameter(description = "驳回操作人") @RequestParam String rejectedBy) {
        return ApiResponse.success(badDebtWriteOffService.reject(id, reason, rejectedBy));
    }

    @Operation(summary = "核销生效", description = "已批准 → 已核销；生效前重新校验账期可写，已关账账期拒绝核销。")
    @PostMapping("/{id}/write-off")
    @AuditLog(operation = "坏账核销生效", businessType = "FINANCE_BAD_DEBT_WRITE_OFF", riskLevel = 4)
    public ApiResponse<BadDebtWriteOff> writeOff(
            @Parameter(description = "核销单 ID") @PathVariable String id,
            @Parameter(description = "核销操作人") @RequestParam String writtenOffBy) {
        return ApiResponse.success(badDebtWriteOffService.writeOff(id, writtenOffBy));
    }
}
