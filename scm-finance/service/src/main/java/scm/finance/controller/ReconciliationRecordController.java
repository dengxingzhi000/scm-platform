package scm.finance.controller;

import com.frog.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.finance.domain.entity.ReconciliationRecord;
import scm.finance.service.IReconciliationRecordService;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/reconciliation-record")
public class ReconciliationRecordController {

    private final IReconciliationRecordService reconciliationRecordService;

    @GetMapping("/{id}")
    public ApiResponse<ReconciliationRecord> getById(@PathVariable String id) {
        ReconciliationRecord record = reconciliationRecordService.getById(id);
        return ApiResponse.success(record);
    }

    @PostMapping
    public ApiResponse<ReconciliationRecord> create(@RequestBody ReconciliationRecord record) {
        ReconciliationRecord created = reconciliationRecordService.createReconciliation(record);
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<ReconciliationRecord> update(@PathVariable String id, @RequestBody ReconciliationRecord record) {
        record.setId(id);
        record.setUpdateTime(java.time.LocalDateTime.now());
        reconciliationRecordService.updateById(record);
        log.info("对账记录更新成功: id={}", id);
        return ApiResponse.success(record);
    }

    @DeleteMapping("/{id}")
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
    public ApiResponse<ReconciliationRecord> reconcile(
            @PathVariable String id,
            @RequestParam String reconcilerId,
            @RequestParam String reconcilerName) {
        ReconciliationRecord record = reconciliationRecordService.reconcile(id, reconcilerId, reconcilerName);
        return ApiResponse.success(record);
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<ReconciliationRecord> confirm(
            @PathVariable String id,
            @RequestParam String confirmerId,
            @RequestParam String confirmerName) {
        ReconciliationRecord record = reconciliationRecordService.confirm(id, confirmerId, confirmerName);
        return ApiResponse.success(record);
    }

    @PostMapping("/{id}/mark-diff")
    public ApiResponse<ReconciliationRecord> markAsDiff(
            @PathVariable String id,
            @RequestParam String diffReason) {
        ReconciliationRecord record = reconciliationRecordService.markAsDiff(id, diffReason);
        return ApiResponse.success(record);
    }
}
