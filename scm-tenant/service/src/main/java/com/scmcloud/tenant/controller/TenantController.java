package com.scmcloud.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.tenant.domain.entity.Tenant;
import com.scmcloud.tenant.service.ITenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/tenant")
@Tag(name = "租户管理", description = "提供租户的创建、查询、更新、删除、启停与分页查询接口。")
public class TenantController {

    private final ITenantService tenantService;

    @Operation(summary = "创建租户", description = "新增一个租户并返回创建后的租户信息。")
    @PostMapping
    public Tenant create(@RequestBody Tenant entity) {
        log.info("[API] 创建租户: tenantCode={}, tenantName={}", entity.getTenantCode(), entity.getTenantName());
        return tenantService.createTenant(entity);
    }

    @Operation(summary = "查询租户详情", description = "根据租户 ID 查询租户详细信息。")
    @GetMapping("/{id}")
    public Tenant getById(@Parameter(description = "租户ID") @PathVariable String id) {
        log.info("[API] 查询租户: id={}", id);
        return tenantService.getById(id);
    }

    @Operation(summary = "更新租户", description = "根据租户 ID 更新租户信息并返回更新结果。")
    @PutMapping
    public Tenant update(@RequestBody Tenant entity) {
        log.info("[API] 更新租户: id={}", entity.getId());
        return tenantService.updateTenant(entity);
    }

    @Operation(summary = "删除租户", description = "根据租户 ID 删除指定租户。")
    @DeleteMapping("/{id}")
    public boolean deleteById(@Parameter(description = "租户ID") @PathVariable String id) {
        log.info("[API] 删除租户: id={}", id);
        return tenantService.deleteById(id);
    }

    @Operation(summary = "启用租户", description = "将指定租户状态置为可用，允许其正常访问平台。")
    @PutMapping("/{id}/enable")
    public boolean enable(@Parameter(description = "租户ID") @PathVariable String id) {
        log.info("[API] 启用租户: id={}", id);
        return tenantService.enableTenant(id);
    }

    @Operation(summary = "禁用租户", description = "将指定租户状态置为禁用，暂停其访问平台。")
    @PutMapping("/{id}/disable")
    public boolean disable(@Parameter(description = "租户ID") @PathVariable String id) {
        log.info("[API] 禁用租户: id={}", id);
        return tenantService.disableTenant(id);
    }

    @Operation(summary = "查询活跃租户列表", description = "查询当前状态为活跃的全部租户。")
    @GetMapping("/active")
    public List<Tenant> listActive() {
        log.info("[API] 查询活跃租户");
        return tenantService.listActive();
    }

    @Operation(summary = "分页查询租户", description = "按名称、类型和状态分页查询租户列表。")
    @GetMapping("/page")
    public Page<Tenant> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "租户名称") @RequestParam(required = false) String tenantName,
            @Parameter(description = "租户类型") @RequestParam(required = false) Integer tenantType,
            @Parameter(description = "租户状态") @RequestParam(required = false) Integer status) {
        log.info("[API] 分页查询租户: page={}, size={}", page, size);
        return tenantService.pageQuery(page, size, tenantName, tenantType, status);
    }
}
