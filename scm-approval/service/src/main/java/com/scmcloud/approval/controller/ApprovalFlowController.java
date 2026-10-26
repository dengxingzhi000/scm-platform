package com.scmcloud.approval.controller;

import com.scmcloud.approval.service.flowable.ApprovalFlowService;
import com.scmcloud.approval.service.flowable.ApprovalFlowService.TaskDTO;
import com.scmcloud.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 审批流程控制器。
 *
 * <p>提供审批流程的REST API：
 * <ul>
 *   <li>发起审批</li>
 *   <li>审批通过/驳回</li>
 *   <li>查询待办任务</li>
 * </ul>
 */
@RestController
@RequestMapping("/approval/flow")
@RequiredArgsConstructor
@Tag(name = "审批流程", description = "基于 Flowable 的审批流程发起、通过、驳回、待办查询与状态查询接口。")
public class ApprovalFlowController {

    private final ApprovalFlowService approvalFlowService;

    /**
     * 发起审批流程。
     */
    @Operation(summary = "发起审批", description = "根据流程定义与业务单据信息发起一个审批流程实例，返回流程实例 ID。")
    @PostMapping("/start")
    public ApiResponse<String> startProcess(@Valid @RequestBody StartProcessRequest request) {
        String processInstanceId = approvalFlowService.startProcess(
                request.getProcessKey(),
                request.getBusinessKey(),
                request.getBusinessType(),
                request.getApplicantId(),
                request.getVariables()
        );
        return ApiResponse.success(processInstanceId);
    }

    /**
     * 审批通过。
     */
    @Operation(summary = "审批通过", description = "提交审批意见，将指定待办任务置为通过并推进流程。")
    @PostMapping("/approve")
    public ApiResponse<Void> approve(@Valid @RequestBody ApproveRequest request) {
        approvalFlowService.approve(
                request.getTaskId(),
                request.getApproverId(),
                request.getComment(),
                request.getVariables()
        );
        return ApiResponse.success();
    }

    /**
     * 审批驳回。
     */
    @Operation(summary = "审批驳回", description = "填写驳回原因，将指定待办任务驳回并结束当前审批分支。")
    @PostMapping("/reject")
    public ApiResponse<Void> reject(@Valid @RequestBody RejectRequest request) {
        approvalFlowService.reject(
                request.getTaskId(),
                request.getApproverId(),
                request.getComment()
        );
        return ApiResponse.success();
    }

    /**
     * 查询待办任务。
     */
    @Operation(summary = "查询待办任务", description = "查询指定审批人名下尚未处理的待办任务列表。")
    @GetMapping("/todo")
    public ApiResponse<List<TaskDTO>> findTodoTasks(@Parameter(description = "审批人用户 ID") @RequestParam @NotBlank String assigneeId) {
        List<TaskDTO> tasks = approvalFlowService.findTodoTasks(assigneeId);
        return ApiResponse.success(tasks);
    }

    /**
     * 查询流程状态。
     */
    @Operation(summary = "查询流程状态", description = "根据业务单据标识查询审批流程状态，返回 COMPLETED、IN_PROGRESS 或 NOT_FOUND。")
    @GetMapping("/status/{businessKey}")
    public ApiResponse<String> getProcessStatus(@Parameter(description = "业务单据唯一标识") @PathVariable String businessKey) {
        var processInstance = approvalFlowService.findByBusinessKey(businessKey);
        if (processInstance == null) {
            return ApiResponse.success("NOT_FOUND");
        }
        return ApiResponse.success(processInstance.isEnded() ? "COMPLETED" : "IN_PROGRESS");
    }

    // ─── Request DTOs ─────────────────────────────────────────────

    @Data
    public static class StartProcessRequest {
        @NotBlank(message = "流程定义key不能为空")
        private String processKey;
        @NotBlank(message = "业务ID不能为空")
        private String businessKey;
        @NotBlank(message = "业务类型不能为空")
        private String businessType;
        @NotBlank(message = "申请人ID不能为空")
        private String applicantId;
        private Map<String, Object> variables;
    }

    @Data
    public static class ApproveRequest {
        @NotBlank(message = "任务ID不能为空")
        private String taskId;
        @NotBlank(message = "审批人ID不能为空")
        private String approverId;
        private String comment;
        private Map<String, Object> variables;
    }

    @Data
    public static class RejectRequest {
        @NotBlank(message = "任务ID不能为空")
        private String taskId;
        @NotBlank(message = "审批人ID不能为空")
        private String approverId;
        @NotBlank(message = "驳回原因不能为空")
        private String comment;
    }
}
