package com.scmcloud.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.tenant.domain.entity.TenantOperationLog;
import com.scmcloud.tenant.service.ITenantOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/tenant-operation-log")
@Tag(name = "租户操作日志", description = "提供租户操作日志的写入、查询、删除与分页检索接口。")
public class TenantOperationLogController {
    private final ITenantOperationLogService tenantOperationLogService;

    @Operation(summary = "创建操作日志", description = "写入一条租户操作日志记录并返回创建结果。")
    @PostMapping
    public TenantOperationLog create(@RequestBody TenantOperationLog entity) {
        log.info("[API] 创建租户操作日志: tenantId={}, operationType={}", (entity.getTenantId() + "").replace("\r", "_").replace("\n", "_"), (entity.getOperationType() + "").replace("\r", "_").replace("\n", "_"));
        return tenantOperationLogService.createLog(entity);
    }

    @Operation(summary = "查询日志详情", description = "根据日志 ID 查询租户操作日志详情。")
    @GetMapping("/{id}")
    public TenantOperationLog getById(@Parameter(description = "日志ID") @PathVariable String id) {
        log.info("[API] 查询租户操作日志: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return tenantOperationLogService.getById(id);
    }

    @Operation(summary = "删除操作日志", description = "根据日志 ID 删除指定的租户操作日志。")
    @DeleteMapping("/{id}")
    public boolean deleteById(@Parameter(description = "日志ID") @PathVariable String id) {
        log.info("[API] 删除租户操作日志: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return tenantOperationLogService.deleteById(id);
    }

    @Operation(summary = "查询租户日志列表", description = "查询指定租户的全部操作日志。")
    @GetMapping("/tenant/{tenantId}")
    public List<TenantOperationLog> listByTenantId(@Parameter(description = "租户ID") @PathVariable String tenantId) {
        log.info("[API] 查询租户操作日志: tenantId={}", (tenantId + "").replace("\r", "_").replace("\n", "_"));
        return tenantOperationLogService.listByTenantId(tenantId);
    }

    @Operation(summary = "按操作类型查询日志", description = "查询指定操作类型下的全部租户操作日志。")
    @GetMapping("/operation-type/{operationType}")
    public List<TenantOperationLog> listByOperationType(@Parameter(description = "操作类型") @PathVariable String operationType) {
        log.info("[API] 查询操作类型日志: operationType={}", (operationType + "").replace("\r", "_").replace("\n", "_"));
        return tenantOperationLogService.listByOperationType(operationType);
    }

    @Operation(summary = "分页查询操作日志", description = "按租户、操作类型和操作模块分页查询租户操作日志。")
    @GetMapping("/page")
    public Page<TenantOperationLog> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "租户ID") @RequestParam(required = false) String tenantId,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operationType,
            @Parameter(description = "操作模块") @RequestParam(required = false) String operationModule) {
        log.info("[API] 分页查询租户操作日志: page={}, size={}", page, size);
        return tenantOperationLogService.pageQuery(page, size, tenantId, operationType, operationModule);
    }
}
