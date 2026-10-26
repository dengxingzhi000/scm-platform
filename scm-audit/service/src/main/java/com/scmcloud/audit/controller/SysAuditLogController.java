package com.scmcloud.audit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.audit.domain.entity.SysAuditLog;
import com.scmcloud.audit.service.ISysAuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/sys-audit-log")
@Tag(name = "审计日志", description = "审计日志的创建、查询、删除与分页检索接口。")
public class SysAuditLogController {
    private final ISysAuditLogService auditLogService;

    @Operation(summary = "创建审计日志", description = "写入一条审计日志记录。")
    @PostMapping
    public SysAuditLog create(@RequestBody SysAuditLog entity) {
        log.info("[API] 创建审计日志: userId={}, operationType={}, module={}",
                entity.getUserId(), entity.getOperationType(), entity.getOperationModule());
        return auditLogService.createLog(entity);
    }

    @Operation(summary = "查询审计日志", description = "根据日志 ID 查询审计日志详情。")
    @GetMapping("/{id}")
    public SysAuditLog getById(@Parameter(description = "审计日志 ID") @PathVariable String id) {
        log.info("[API] 查询审计日志: id={}", id);
        return auditLogService.getById(id);
    }

    @Operation(summary = "删除审计日志", description = "根据日志 ID 删除审计日志记录。")
    @DeleteMapping("/{id}")
    public boolean deleteById(@Parameter(description = "审计日志 ID") @PathVariable String id) {
        log.info("[API] 删除审计日志: id={}", id);
        return auditLogService.deleteById(id);
    }

    @Operation(summary = "按用户查询审计日志", description = "查询指定用户产生的全部审计日志。")
    @GetMapping("/user/{userId}")
    public List<SysAuditLog> listByUserId(@Parameter(description = "用户 ID") @PathVariable String userId) {
        log.info("[API] 查询用户审计日志: userId={}", userId);
        return auditLogService.listByUserId(userId);
    }

    @Operation(summary = "按模块查询审计日志", description = "查询指定业务模块的审计日志。")
    @GetMapping("/module/{module}")
    public List<SysAuditLog> listByModule(@Parameter(description = "业务模块名称") @PathVariable String module) {
        log.info("[API] 查询模块审计日志: module={}", module);
        return auditLogService.listByModule(module);
    }

    @Operation(summary = "按业务类型查询审计日志", description = "查询指定业务类型的审计日志。")
    @GetMapping("/business-type/{businessType}")
    public List<SysAuditLog> listByBusinessType(@Parameter(description = "业务类型") @PathVariable String businessType) {
        log.info("[API] 查询业务类型审计日志: businessType={}", businessType);
        return auditLogService.listByBusinessType(businessType);
    }

    @Operation(summary = "分页查询审计日志", description = "按用户、操作类型、模块、业务类型和风险等级分页查询审计日志。")
    @GetMapping("/page")
    public Page<SysAuditLog> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "用户 ID") @RequestParam(required = false) String userId,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operationType,
            @Parameter(description = "业务模块名称") @RequestParam(required = false) String module,
            @Parameter(description = "业务类型") @RequestParam(required = false) String businessType,
            @Parameter(description = "风险等级") @RequestParam(required = false) Integer riskLevel) {
        log.info("[API] 分页查询审计日志: page={}, size={}", page, size);
        return auditLogService.pageQuery(page, size, userId, operationType, module, businessType, riskLevel);
    }
}
