package com.scmcloud.audit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.audit.domain.entity.SysSensitiveOperationLog;
import com.scmcloud.audit.service.ISysSensitiveOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/sys-sensitive-operation-log")
@Tag(name = "敏感操作日志", description = "敏感操作日志的创建、查询、删除与分页检索接口。")
public class SysSensitiveOperationLogController {

    private final ISysSensitiveOperationLogService sensitiveOperationLogService;

    @Operation(summary = "创建敏感操作日志", description = "写入一条敏感操作日志记录。")
    @PostMapping
    public SysSensitiveOperationLog create(@RequestBody SysSensitiveOperationLog entity) {
        log.info("[API] 创建敏感操作日志: userId={}, operationType={}", entity.getUserId(), entity.getOperationType());
        return sensitiveOperationLogService.createLog(entity);
    }

    @Operation(summary = "查询敏感操作日志", description = "根据日志 ID 查询敏感操作日志详情。")
    @GetMapping("/{id}")
    public SysSensitiveOperationLog getById(@Parameter(description = "敏感操作日志 ID") @PathVariable String id) {
        log.info("[API] 查询敏感操作日志: id={}", id);
        return sensitiveOperationLogService.getById(id);
    }

    @Operation(summary = "删除敏感操作日志", description = "根据日志 ID 删除敏感操作日志记录。")
    @DeleteMapping("/{id}")
    public boolean deleteById(@Parameter(description = "敏感操作日志 ID") @PathVariable String id) {
        log.info("[API] 删除敏感操作日志: id={}", id);
        return sensitiveOperationLogService.deleteById(id);
    }

    @Operation(summary = "按用户查询敏感操作日志", description = "查询指定用户产生的全部敏感操作日志。")
    @GetMapping("/user/{userId}")
    public List<SysSensitiveOperationLog> listByUserId(@Parameter(description = "用户 ID") @PathVariable String userId) {
        log.info("[API] 查询用户敏感操作日志: userId={}", userId);
        return sensitiveOperationLogService.listByUserId(userId);
    }

    @Operation(summary = "按风险等级查询敏感操作日志", description = "查询指定风险分值的敏感操作日志。")
    @GetMapping("/risk-level/{riskScore}")
    public List<SysSensitiveOperationLog> listByRiskLevel(@Parameter(description = "风险分值") @PathVariable Integer riskScore) {
        log.info("[API] 查询风险等级敏感操作日志: riskScore={}", riskScore);
        return sensitiveOperationLogService.listByRiskLevel(riskScore);
    }

    @Operation(summary = "分页查询敏感操作日志", description = "按用户、操作类型和风险分值分页查询敏感操作日志。")
    @GetMapping("/page")
    public Page<SysSensitiveOperationLog> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "用户 ID") @RequestParam(required = false) String userId,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operationType,
            @Parameter(description = "风险分值") @RequestParam(required = false) Integer riskScore) {
        log.info("[API] 分页查询敏感操作日志: page={}, size={}", page, size);
        return sensitiveOperationLogService.pageQuery(page, size, userId, operationType, riskScore);
    }
}
