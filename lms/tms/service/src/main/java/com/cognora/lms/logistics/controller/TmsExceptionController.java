package com.cognora.lms.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.lms.logistics.domain.entity.TmsClaimOrder;
import com.cognora.lms.logistics.domain.entity.TmsExceptionCase;
import com.cognora.lms.logistics.service.ITmsClaimOrderService;
import com.cognora.lms.logistics.service.ITmsExceptionCaseService;
import com.cognora.platform.common.log.annotation.AuditLog;
import com.cognora.platform.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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

import java.math.BigDecimal;

/**
 * 异常件认领闭环与理赔单据接口（批次 3）。
 */
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/tms-exception")
@Tag(name = "异常件与理赔", description = "异常件登记、责任方认领、处理闭环与理赔单据状态流转。")
public class TmsExceptionController {

    private final ITmsExceptionCaseService exceptionCaseService;
    private final ITmsClaimOrderService claimOrderService;

    public record CreateCaseRequest(String waybillId, String exceptionType, String description, String operator) {
    }

    public record ClaimRequest(String responsibleParty, String operator) {
    }

    public record CaseTransitionRequest(String targetStatus, String resolution, String operator) {
    }

    public record CreateClaimRequest(BigDecimal amount, String reason, String evidenceUrl, String operator) {
    }

    public record ClaimTransitionRequest(String targetStatus, String operator, String remark) {
    }

    @Operation(summary = "分页查询异常件")
    @GetMapping("/list")
    public ApiResponse<Page<TmsExceptionCase>> pageCases(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String waybillNo,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(exceptionCaseService.pageList(page, size, waybillNo, status));
    }

    @Operation(summary = "登记异常件")
    @PostMapping
    @AuditLog(operation = "登记异常件", businessType = "LOGISTICS", riskLevel = 2)
    public ApiResponse<TmsExceptionCase> createCase(@RequestBody CreateCaseRequest request) {
        return ApiResponse.success(exceptionCaseService.createCase(
                request.waybillId(), request.exceptionType(), request.description(), request.operator()));
    }

    @Operation(summary = "认领异常件")
    @PostMapping("/{id}/claim")
    @AuditLog(operation = "认领异常件", businessType = "LOGISTICS", riskLevel = 2)
    public ApiResponse<TmsExceptionCase> claim(@PathVariable String id, @RequestBody ClaimRequest request) {
        return ApiResponse.success(exceptionCaseService.claim(
                id, request.responsibleParty(), request.operator()));
    }

    @Operation(summary = "异常件状态流转")
    @PostMapping("/{id}/transition")
    @AuditLog(operation = "异常件状态流转", businessType = "LOGISTICS", riskLevel = 2)
    public ApiResponse<TmsExceptionCase> transition(@PathVariable String id,
                                                    @RequestBody CaseTransitionRequest request) {
        return ApiResponse.success(exceptionCaseService.transition(
                id, request.targetStatus(), request.resolution(), request.operator()));
    }

    @Operation(summary = "创建理赔单")
    @PostMapping("/{id}/claims")
    @AuditLog(operation = "创建理赔单", businessType = "LOGISTICS", riskLevel = 3)
    public ApiResponse<TmsClaimOrder> createClaim(@PathVariable String id,
                                                  @RequestBody CreateClaimRequest request) {
        return ApiResponse.success(claimOrderService.createClaim(
                id, request.amount(), request.reason(), request.evidenceUrl(), request.operator()));
    }

    @Operation(summary = "分页查询理赔单")
    @GetMapping("/claims/list")
    public ApiResponse<Page<TmsClaimOrder>> pageClaims(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String waybillNo,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(claimOrderService.pageList(page, size, waybillNo, status));
    }

    @Operation(summary = "理赔单状态流转")
    @PostMapping("/claims/{id}/transition")
    @AuditLog(operation = "理赔单状态流转", businessType = "LOGISTICS", riskLevel = 3)
    public ApiResponse<TmsClaimOrder> transitionClaim(@PathVariable String id,
                                                      @RequestBody ClaimTransitionRequest request) {
        return ApiResponse.success(claimOrderService.transition(
                id, request.targetStatus(), request.operator(), request.remark()));
    }
}
