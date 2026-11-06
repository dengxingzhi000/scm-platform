package com.cognora.lms.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.lms.logistics.domain.entity.TmsSlaAlert;
import com.cognora.lms.logistics.service.ITmsSlaAlertService;
import com.cognora.platform.common.log.annotation.AuditLog;
import com.cognora.platform.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * SLA 预警查询与确认接口（批次 3）。定时扫描由 {@code tmsSlaOverdueScanHandler} 执行。
 */
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/tms-sla")
@Tag(name = "SLA 时效预警", description = "超时预警查询、确认与手动触发扫描。")
public class TmsSlaController {

    private final ITmsSlaAlertService slaAlertService;

    @Operation(summary = "分页查询 SLA 预警")
    @GetMapping("/list")
    public ApiResponse<Page<TmsSlaAlert>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String waybillNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level) {
        return ApiResponse.success(slaAlertService.pageList(page, size, waybillNo, status, level));
    }

    @Operation(summary = "确认预警")
    @PostMapping("/{id}/ack")
    @AuditLog(operation = "确认 SLA 预警", businessType = "LOGISTICS", riskLevel = 1)
    public ApiResponse<Void> acknowledge(@PathVariable String id,
                                         @RequestParam(required = false) String operator) {
        slaAlertService.acknowledge(id, operator);
        return ApiResponse.success();
    }

    @Operation(summary = "手动触发超时扫描", description = "与 XXL-Job 同一逻辑，便于运维补扫。")
    @PostMapping("/scan")
    @AuditLog(operation = "手动触发 SLA 扫描", businessType = "LOGISTICS", riskLevel = 1)
    public ApiResponse<Integer> scan() {
        return ApiResponse.success(slaAlertService.scanOverdue(LocalDateTime.now()));
    }
}
