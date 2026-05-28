package scm.finance.controller;

import com.frog.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.finance.domain.entity.ReconciliationRecord;
import scm.finance.service.IReconciliationRecordService;

@Slf4j
@RestController
@RequestMapping("/reconciliation-record")
@Tag(name = "对账记录", description = "对账记录管理接口")
public class ReconciliationRecordController {

    @Autowired
    private IReconciliationRecordService reconciliationRecordService;

    @GetMapping("/{id}")
    @Operation(summary = "查询对账记录详情")
    public ApiResponse<ReconciliationRecord> getById(@PathVariable String id) {
        ReconciliationRecord record = reconciliationRecordService.getById(id);
        return ApiResponse.success(record);
    }

    @PostMapping
    @Operation(summary = "创建对账记录")
    public ApiResponse<ReconciliationRecord> create(@RequestBody ReconciliationRecord record) {
        ReconciliationRecord created = reconciliationRecordService.createReconciliation(record);
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新对账记录")
    public ApiResponse<ReconciliationRecord> update(@PathVariable String id, @RequestBody ReconciliationRecord record) {
        record.setId(id);
        record.setUpdateTime(java.time.LocalDateTime.now());
        reconciliationRecordService.updateById(record);
        log.info("对账记录更新成功: id={}", id);
        return ApiResponse.success(record);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除对账记录")
    public ApiResponse<Void> delete(@PathVariable String id) {
        ReconciliationRecord record = reconciliationRecordService.getById(id);
        if (record != null) {
            record.setDeleted(true);
            record.setUpdateTime(java.time.LocalDateTime.now());
            reconciliationRecordService.updateById(record);
            log.info("对账记录删除成功: id={}", id);
        }
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reconcile")
    @Operation(summary = "执行对账")
    public ApiResponse<ReconciliationRecord> reconcile(
            @PathVariable String id,
            @Parameter(description = "对账人ID", required = true) @RequestParam String reconcilerId,
            @Parameter(description = "对账人姓名", required = true) @RequestParam String reconcilerName) {
        ReconciliationRecord record = reconciliationRecordService.reconcile(id, reconcilerId, reconcilerName);
        return ApiResponse.success(record);
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "确认对账")
    public ApiResponse<ReconciliationRecord> confirm(
            @PathVariable String id,
            @Parameter(description = "确认人ID", required = true) @RequestParam String confirmerId,
            @Parameter(description = "确认人姓名", required = true) @RequestParam String confirmerName) {
        ReconciliationRecord record = reconciliationRecordService.confirm(id, confirmerId, confirmerName);
        return ApiResponse.success(record);
    }

    @PostMapping("/{id}/mark-diff")
    @Operation(summary = "标记对账差异")
    public ApiResponse<ReconciliationRecord> markAsDiff(
            @PathVariable String id,
            @Parameter(description = "差异原因", required = true) @RequestParam String diffReason) {
        ReconciliationRecord record = reconciliationRecordService.markAsDiff(id, diffReason);
        return ApiResponse.success(record);
    }
}
