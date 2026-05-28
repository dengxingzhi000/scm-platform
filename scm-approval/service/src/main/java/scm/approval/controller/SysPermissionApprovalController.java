package scm.approval.controller;

import com.frog.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import scm.approval.domain.entity.SysPermissionApproval;
import scm.approval.service.ISysPermissionApprovalService;

import java.util.List;

@RestController
@RequestMapping("/sys-permission-approval")
@RequiredArgsConstructor
public class SysPermissionApprovalController {

    private final ISysPermissionApprovalService approvalService;

    @PostMapping("/submit")
    public ApiResponse<SysPermissionApproval> submitApproval(@RequestBody SysPermissionApproval approval) {
        SysPermissionApproval result = approvalService.submitApproval(approval);
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<SysPermissionApproval> approve(
            @PathVariable String id,
            @RequestParam String approverId,
            @RequestParam String approverName) {
        SysPermissionApproval result = approvalService.approve(id, approverId, approverName);
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<SysPermissionApproval> reject(
            @PathVariable String id,
            @RequestParam String approverId,
            @RequestParam String approverName,
            @RequestParam String rejectReason) {
        SysPermissionApproval result = approvalService.reject(id, approverId, approverName, rejectReason);
        return ApiResponse.success(result);
    }

    @GetMapping("/applicant/{applicantId}")
    public ApiResponse<List<SysPermissionApproval>> listByApplicant(@PathVariable String applicantId) {
        List<SysPermissionApproval> result = approvalService.listByApplicant(applicantId);
        return ApiResponse.success(result);
    }

    @GetMapping("/pending")
    public ApiResponse<List<SysPermissionApproval>> listPending(@RequestParam String approverId) {
        List<SysPermissionApproval> result = approvalService.listPending(approverId);
        return ApiResponse.success(result);
    }
}
