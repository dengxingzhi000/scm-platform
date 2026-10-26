package com.scmcloud.approval.controller;

import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.approval.domain.entity.SysPermissionApproval;
import com.scmcloud.approval.service.ISysPermissionApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/sys-permission-approval")
@RequiredArgsConstructor
@Tag(name = "权限审批", description = "权限变更申请的提交、审批、驳回及按申请人或审批人查询的接口。")
public class SysPermissionApprovalController {

    private final ISysPermissionApprovalService approvalService;

    @Operation(summary = "提交权限审批", description = "提交一条权限变更申请，进入待审批状态。")
    @PostMapping("/submit")
    public ApiResponse<SysPermissionApproval> submitApproval(@RequestBody SysPermissionApproval approval) {
        SysPermissionApproval result = approvalService.submitApproval(approval);
        return ApiResponse.success(result);
    }

    @Operation(summary = "通过权限审批", description = "审批人通过指定的权限变更申请并记录审批信息。")
    @PostMapping("/{id}/approve")
    public ApiResponse<SysPermissionApproval> approve(
            @Parameter(description = "权限审批单 ID") @PathVariable String id,
            @Parameter(description = "审批人用户 ID") @RequestParam String approverId,
            @Parameter(description = "审批人姓名") @RequestParam String approverName) {
        SysPermissionApproval result = approvalService.approve(id, approverId, approverName);
        return ApiResponse.success(result);
    }

    @Operation(summary = "驳回权限审批", description = "审批人驳回指定的权限变更申请并填写驳回原因。")
    @PostMapping("/{id}/reject")
    public ApiResponse<SysPermissionApproval> reject(
            @Parameter(description = "权限审批单 ID") @PathVariable String id,
            @Parameter(description = "审批人用户 ID") @RequestParam String approverId,
            @Parameter(description = "审批人姓名") @RequestParam String approverName,
            @Parameter(description = "驳回原因") @RequestParam String rejectReason) {
        SysPermissionApproval result = approvalService.reject(id, approverId, approverName, rejectReason);
        return ApiResponse.success(result);
    }

    @Operation(summary = "查询申请人审批单", description = "查询指定申请人提交的全部权限审批单。")
    @GetMapping("/applicant/{applicantId}")
    public ApiResponse<List<SysPermissionApproval>> listByApplicant(@Parameter(description = "申请人用户 ID") @PathVariable String applicantId) {
        List<SysPermissionApproval> result = approvalService.listByApplicant(applicantId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "查询待审批单", description = "查询指定审批人名下处于待审批状态的权限审批单。")
    @GetMapping("/pending")
    public ApiResponse<List<SysPermissionApproval>> listPending(@Parameter(description = "审批人用户 ID") @RequestParam String approverId) {
        List<SysPermissionApproval> result = approvalService.listPending(approverId);
        return ApiResponse.success(result);
    }
}
