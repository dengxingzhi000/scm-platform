package com.scmcloud.system.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.system.domain.entity.SysStatusDict;
import com.scmcloud.system.domain.entity.SysStatusTransition;
import com.scmcloud.system.service.ISysStatusDictService;
import com.scmcloud.system.statemachine.AvailableAction;
import com.scmcloud.system.statemachine.StateMachineEngine;
import com.scmcloud.system.statemachine.TransitionCheckResult;
import com.scmcloud.system.statemachine.TransitionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/system/status")
@RequiredArgsConstructor
@Tag(name = "状态字典", description = "各业务类型的状态定义与状态流转规则的维护，并提供状态机流转校验与执行。")
public class SysStatusDictController {
    private final ISysStatusDictService statusDictService;
    private final StateMachineEngine stateMachineEngine;

    @Operation(summary = "查询业务状态字典", description = "根据业务类型查询该业务下的全部状态定义。")
    @GetMapping("/dict/{bizType}")
    public ApiResponse<List<SysStatusDict>> listStatusDict(
            @Parameter(description = "业务类型，如 ORDER、PURCHASE、APPROVAL") @PathVariable String bizType) {
        return ApiResponse.success(statusDictService.listStatusDict(bizType));
    }

    @Operation(summary = "查询状态详情", description = "根据业务类型与状态编码查询单个状态定义。")
    @GetMapping("/dict/{bizType}/{statusCode}")
    public ApiResponse<SysStatusDict> getStatusByCode(
            @Parameter(description = "业务类型，如 ORDER、PURCHASE、APPROVAL") @PathVariable String bizType,
            @Parameter(description = "状态编码") @PathVariable String statusCode) {
        return ApiResponse.success(statusDictService.getStatusByCode(bizType, statusCode));
    }

    @Operation(summary = "新增状态字典", description = "新增一条业务状态定义，主键由系统自动生成。")
    @PostMapping("/dict")
    @PreAuthorize("hasAuthority('system:status:add')")
    public ApiResponse<Void> createStatusDict(@RequestBody SysStatusDict entity) {
        entity.setId(UUID.randomUUID().toString());
        statusDictService.createStatusDict(entity);
        return ApiResponse.success(null);
    }

    @Operation(summary = "修改状态字典", description = "更新已有业务状态定义的信息。")
    @PutMapping("/dict")
    @PreAuthorize("hasAuthority('system:status:edit')")
    public ApiResponse<Void> updateStatusDict(@RequestBody SysStatusDict entity) {
        statusDictService.updateStatusDict(entity);
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除状态字典", description = "根据状态 ID 删除该业务状态定义。")
    @DeleteMapping("/dict/{id}")
    @PreAuthorize("hasAuthority('system:status:delete')")
    public ApiResponse<Void> deleteStatusDict(@Parameter(description = "状态定义 ID") @PathVariable String id) {
        statusDictService.deleteStatusDict(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "查询状态流转规则", description = "根据业务类型查询该业务下全部允许的状态流转规则。")
    @GetMapping("/transitions/{bizType}")
    public ApiResponse<List<SysStatusTransition>> listTransitions(
            @Parameter(description = "业务类型，如 ORDER、PURCHASE、APPROVAL") @PathVariable String bizType) {
        return ApiResponse.success(statusDictService.listTransitions(bizType));
    }

    @Operation(summary = "新增状态流转规则", description = "新增一条状态流转规则，主键由系统自动生成。")
    @PostMapping("/transitions")
    @PreAuthorize("hasAuthority('system:status:add')")
    public ApiResponse<Void> createTransition(@RequestBody SysStatusTransition entity) {
        entity.setId(UUID.randomUUID().toString());
        statusDictService.createTransition(entity);
        return ApiResponse.success(null);
    }

    @Operation(summary = "修改状态流转规则", description = "更新已有状态流转规则的起始状态、目标状态与动作等信息。")
    @PutMapping("/transitions")
    @PreAuthorize("hasAuthority('system:status:edit')")
    public ApiResponse<Void> updateTransition(@RequestBody SysStatusTransition entity) {
        statusDictService.updateTransition(entity);
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除状态流转规则", description = "根据流转规则 ID 删除该状态流转规则。")
    @DeleteMapping("/transitions/{id}")
    @PreAuthorize("hasAuthority('system:status:delete')")
    public ApiResponse<Void> deleteTransition(@Parameter(description = "状态流转规则 ID") @PathVariable String id) {
        statusDictService.deleteTransition(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "校验状态流转", description = "校验指定业务能否从当前状态直接流转到目标状态。")
    @GetMapping("/check")
    public ApiResponse<TransitionCheckResult> checkTransition(
            @Parameter(description = "业务类型，如 ORDER、PURCHASE、APPROVAL") @RequestParam String bizType,
            @Parameter(description = "当前状态编码") @RequestParam String fromStatus,
            @Parameter(description = "目标状态编码") @RequestParam String toStatus) {
        return ApiResponse.success(stateMachineEngine.canTransition(bizType, fromStatus, toStatus));
    }

    @Operation(summary = "执行状态流转", description = "按业务类型、当前状态与动作编码执行一次状态流转并返回流转结果。")
    @PostMapping("/transition")
    public ApiResponse<TransitionResult> executeTransition(
            @Parameter(description = "业务类型，如 ORDER、PURCHASE、APPROVAL") @RequestParam String bizType,
            @Parameter(description = "当前状态编码") @RequestParam String fromStatus,
            @Parameter(description = "动作编码，如 PAY、SHIP") @RequestParam String actionCode) {
        return ApiResponse.success(stateMachineEngine.transition(bizType, fromStatus, actionCode));
    }

    @Operation(summary = "查询可执行动作", description = "查询指定业务在当前状态下可执行的动作列表。")
    @GetMapping("/actions")
    public ApiResponse<List<AvailableAction>> getAvailableActions(
            @Parameter(description = "业务类型，如 ORDER、PURCHASE、APPROVAL") @RequestParam String bizType,
            @Parameter(description = "当前状态编码") @RequestParam String currentStatus) {
        return ApiResponse.success(stateMachineEngine.getAvailableActions(bizType, currentStatus));
    }

    @Operation(summary = "查询下一状态", description = "查询指定业务在当前状态下允许流转到的下一状态列表。")
    @GetMapping("/next-statuses")
    public ApiResponse<List<String>> getValidNextStatuses(
            @Parameter(description = "业务类型，如 ORDER、PURCHASE、APPROVAL") @RequestParam String bizType,
            @Parameter(description = "当前状态编码") @RequestParam String currentStatus) {
        return ApiResponse.success(stateMachineEngine.getValidNextStatuses(bizType, currentStatus));
    }
}
