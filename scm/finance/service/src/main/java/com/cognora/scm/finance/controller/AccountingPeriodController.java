package com.cognora.scm.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.log.annotation.AuditLog;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.scm.finance.domain.entity.AccountingPeriod;
import com.cognora.scm.finance.service.AccountingPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/accounting-period")
@Tag(name = "会计期间", description = "开启与关闭会计期间；关账后该期间单据禁止写入。")
public class AccountingPeriodController {

    private final AccountingPeriodService accountingPeriodService;

    @Operation(summary = "开启账期", description = "按 YYYY-MM 开启账期，起止日期由月份自动推导。")
    @PostMapping
    @AuditLog(operation = "开启账期", businessType = "FINANCE_ACCOUNTING_PERIOD", riskLevel = 3)
    public ApiResponse<AccountingPeriod> open(
            @Parameter(description = "账期，格式 YYYY-MM") @RequestParam String periodCode) {
        return ApiResponse.success(accountingPeriodService.openPeriod(periodCode));
    }

    @Operation(summary = "关闭账期", description = "将未结账账期置为已结账，非法流转返回 409。")
    @PostMapping("/{id}/close")
    @AuditLog(operation = "关闭账期", businessType = "FINANCE_ACCOUNTING_PERIOD", riskLevel = 3)
    public ApiResponse<AccountingPeriod> close(
            @Parameter(description = "账期 ID") @PathVariable String id,
            @Parameter(description = "关账操作人 ID") @RequestParam String closedBy) {
        return ApiResponse.success(accountingPeriodService.closePeriod(id, closedBy));
    }

    @Operation(summary = "查询账期详情", description = "根据账期 ID 查询账期状态与起止日期。")
    @GetMapping("/{id}")
    public ApiResponse<AccountingPeriod> getById(@Parameter(description = "账期 ID") @PathVariable String id) {
        return ApiResponse.success(accountingPeriodService.getById(id));
    }

    @Operation(summary = "查询当前账期", description = "返回当前自然月对应的账期；当月尚未开账时 data 为空。")
    @GetMapping("/current")
    public ApiResponse<AccountingPeriod> current() {
        return ApiResponse.success(accountingPeriodService.getCurrentPeriod());
    }

    @Operation(summary = "查询已关账期列表", description = "返回当前租户下所有已关账账期，按账期编码倒序。")
    @GetMapping("/closed")
    public ApiResponse<List<AccountingPeriod>> closed() {
        return ApiResponse.success(accountingPeriodService.listClosedPeriods());
    }

    @Operation(summary = "分页查询账期", description = "按状态分页查询账期，状态为空时查询全部。")
    @GetMapping("/page")
    public ApiResponse<Page<AccountingPeriod>> page(
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量，默认 20") @RequestParam(required = false) Integer size,
            @Parameter(description = "账期状态 0-未结账 1-已结账") @RequestParam(required = false) Integer status) {
        return ApiResponse.success(accountingPeriodService.pageQuery(
                page == null ? 1 : page, size == null ? 20 : size, status));
    }
}
