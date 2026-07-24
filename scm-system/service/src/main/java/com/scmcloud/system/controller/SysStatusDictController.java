package com.scmcloud.system.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.system.domain.entity.SysStatusDict;
import com.scmcloud.system.domain.entity.SysStatusTransition;
import com.scmcloud.system.service.ISysStatusDictService;
import com.scmcloud.system.statemachine.AvailableAction;
import com.scmcloud.system.statemachine.StateMachineEngine;
import com.scmcloud.system.statemachine.TransitionCheckResult;
import com.scmcloud.system.statemachine.TransitionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/system/status")
@RequiredArgsConstructor
public class SysStatusDictController {
    private final ISysStatusDictService statusDictService;
    private final StateMachineEngine stateMachineEngine;

    @GetMapping("/dict/{bizType}")
    public ApiResponse<List<SysStatusDict>> listStatusDict(@PathVariable String bizType) {
        return ApiResponse.success(statusDictService.listStatusDict(bizType));
    }

    @GetMapping("/dict/{bizType}/{statusCode}")
    public ApiResponse<SysStatusDict> getStatusByCode(
            @PathVariable String bizType, @PathVariable String statusCode) {
        return ApiResponse.success(statusDictService.getStatusByCode(bizType, statusCode));
    }

    @PostMapping("/dict")
    @PreAuthorize("hasAuthority('system:status:add')")
    public ApiResponse<Void> createStatusDict(@RequestBody SysStatusDict entity) {
        entity.setId(UUID.randomUUID().toString());
        statusDictService.createStatusDict(entity);
        return ApiResponse.success(null);
    }

    @PutMapping("/dict")
    @PreAuthorize("hasAuthority('system:status:edit')")
    public ApiResponse<Void> updateStatusDict(@RequestBody SysStatusDict entity) {
        statusDictService.updateStatusDict(entity);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/dict/{id}")
    @PreAuthorize("hasAuthority('system:status:delete')")
    public ApiResponse<Void> deleteStatusDict(@PathVariable String id) {
        statusDictService.deleteStatusDict(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/transitions/{bizType}")
    public ApiResponse<List<SysStatusTransition>> listTransitions(@PathVariable String bizType) {
        return ApiResponse.success(statusDictService.listTransitions(bizType));
    }

    @PostMapping("/transitions")
    @PreAuthorize("hasAuthority('system:status:add')")
    public ApiResponse<Void> createTransition(@RequestBody SysStatusTransition entity) {
        entity.setId(UUID.randomUUID().toString());
        statusDictService.createTransition(entity);
        return ApiResponse.success(null);
    }

    @PutMapping("/transitions")
    @PreAuthorize("hasAuthority('system:status:edit')")
    public ApiResponse<Void> updateTransition(@RequestBody SysStatusTransition entity) {
        statusDictService.updateTransition(entity);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/transitions/{id}")
    @PreAuthorize("hasAuthority('system:status:delete')")
    public ApiResponse<Void> deleteTransition(@PathVariable String id) {
        statusDictService.deleteTransition(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/check")
    public ApiResponse<TransitionCheckResult> checkTransition(
            @RequestParam String bizType,
            @RequestParam String fromStatus,
            @RequestParam String toStatus) {
        return ApiResponse.success(stateMachineEngine.canTransition(bizType, fromStatus, toStatus));
    }

    @PostMapping("/transition")
    public ApiResponse<TransitionResult> executeTransition(
            @RequestParam String bizType,
            @RequestParam String fromStatus,
            @RequestParam String actionCode) {
        return ApiResponse.success(stateMachineEngine.transition(bizType, fromStatus, actionCode));
    }

    @GetMapping("/actions")
    public ApiResponse<List<AvailableAction>> getAvailableActions(
            @RequestParam String bizType,
            @RequestParam String currentStatus) {
        return ApiResponse.success(stateMachineEngine.getAvailableActions(bizType, currentStatus));
    }

    @GetMapping("/next-statuses")
    public ApiResponse<List<String>> getValidNextStatuses(
            @RequestParam String bizType,
            @RequestParam String currentStatus) {
        return ApiResponse.success(stateMachineEngine.getValidNextStatuses(bizType, currentStatus));
    }
}
