package com.cognora.scm.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.cognora.scm.finance.domain.entity.ReconciliationRecord;
import com.cognora.scm.finance.service.IReconciliationRecordService;
import com.cognora.scm.finance.service.query.ReconciliationRecordQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/reconciliation-record")
@Tag(name = "对账记录", description = "管理双方对账记录的创建、对账、确认与差异标记。")
public class ReconciliationRecordController {
    private final IReconciliationRecordService reconciliationRecordService;
    private final ReconciliationRecordQueryService reconciliationRecordQueryService;

    @Operation(summary = "分页查询对账记录", description = "按单号/往来单位关键字、账期、状态和差异标记分页查询对账记录。")
    @GetMapping("/page")
    public ApiResponse<Page<ReconciliationRecord>> page(
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量，默认 20") @RequestParam(required = false) Integer size,
            @Parameter(description = "单号或往来单位关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "对账账期") @RequestParam(required = false) String reconciliationPeriod,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "是否差异") @RequestParam(required = false) Boolean hasDiff) {
        return ApiResponse.success(reconciliationRecordQueryService.pageQuery(
                page == null ? 1 : page, size == null ? 20 : size,
                keyword, reconciliationPeriod, status, hasDiff));
    }

    @Operation(summary = "查询对账记录详情", description = "根据对账记录 ID 查询双方对账的完整信息。")
    @GetMapping("/{id}")
    public ApiResponse<ReconciliationRecord> getById(@Parameter(description = "对账记录 ID") @PathVariable String id) {
        ReconciliationRecord record = reconciliationRecordService.getById(id);
        return ApiResponse.success(record);
    }

    @Operation(summary = "创建对账记录", description = "根据对账数据创建对账记录并计算双方金额差异。")
    @PostMapping
    public ApiResponse<ReconciliationRecord> create(@RequestBody ReconciliationRecord record) {
        ReconciliationRecord created = reconciliationRecordService.createReconciliation(record);
        return ApiResponse.success(created);
    }

    @Operation(summary = "更新对账记录", description = "按对账记录 ID 更新对账信息并刷新更新时间。")
    @PutMapping("/{id}")
    public ApiResponse<ReconciliationRecord> update(@Parameter(description = "对账记录 ID") @PathVariable String id, @RequestBody ReconciliationRecord record) {
        ReconciliationRecord updated = reconciliationRecordService.updateReconciliation(id, record);
        return ApiResponse.success(updated);
    }

    @Operation(summary = "删除对账记录", description = "按对账记录 ID 对对账记录执行逻辑删除。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "对账记录 ID") @PathVariable String id) {
        reconciliationRecordService.deleteReconciliation(id);
        return ApiResponse.success();
    }

    @Operation(summary = "执行对账", description = "记录对账人与对账结果，将对账记录置为已对账状态。")
    @PostMapping("/{id}/reconcile")
    public ApiResponse<ReconciliationRecord> reconcile(
            @Parameter(description = "对账记录 ID") @PathVariable String id,
            @Parameter(description = "对账操作人 ID") @RequestParam String reconcilerId,
            @Parameter(description = "对账操作人姓名") @RequestParam String reconcilerName) {
        ReconciliationRecord record = reconciliationRecordService.reconcile(id, reconcilerId, reconcilerName);
        return ApiResponse.success(record);
    }

    @Operation(summary = "确认对账", description = "记录确认人并确认对账结果，完成对账流程。")
    @PostMapping("/{id}/confirm")
    public ApiResponse<ReconciliationRecord> confirm(
            @Parameter(description = "对账记录 ID") @PathVariable String id,
            @Parameter(description = "对账确认人 ID") @RequestParam String confirmerId,
            @Parameter(description = "对账确认人姓名") @RequestParam String confirmerName) {
        ReconciliationRecord record = reconciliationRecordService.confirm(id, confirmerId, confirmerName);
        return ApiResponse.success(record);
    }

    @Operation(summary = "标记对账差异", description = "将对账记录标记为存在差异并记录差异原因。")
    @PostMapping("/{id}/mark-diff")
    public ApiResponse<ReconciliationRecord> markAsDiff(
            @Parameter(description = "对账记录 ID") @PathVariable String id,
            @Parameter(description = "差异原因说明") @RequestParam String diffReason) {
        ReconciliationRecord record = reconciliationRecordService.markAsDiff(id, diffReason);
        return ApiResponse.success(record);
    }
}
