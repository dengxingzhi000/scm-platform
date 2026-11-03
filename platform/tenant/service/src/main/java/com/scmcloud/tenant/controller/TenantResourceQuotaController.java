package com.scmcloud.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.tenant.domain.entity.TenantResourceQuota;
import com.scmcloud.tenant.service.ITenantResourceQuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/tenant-resource-quota")
@Tag(name = "租户资源配额", description = "提供租户资源配额的维护、配额余量校验与分页查询接口。")
public class TenantResourceQuotaController {
    private final ITenantResourceQuotaService tenantResourceQuotaService;

    @Operation(summary = "创建资源配额", description = "为指定租户新增资源配额记录并返回创建结果。")
    @PostMapping
    public TenantResourceQuota create(@RequestBody TenantResourceQuota entity) {
        log.info("[API] Create tenant resource quota: tenantId={}", (entity.getTenantId() + "").replace("\r", "_").replace("\n", "_"));
        return tenantResourceQuotaService.createQuota(entity);
    }

    @Operation(summary = "查询配额详情", description = "根据配额 ID 查询租户资源配额详情。")
    @GetMapping("/{id}")
    public TenantResourceQuota getById(@Parameter(description = "配额ID") @PathVariable String id) {
        log.info("[API] Query tenant resource quota: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return tenantResourceQuotaService.getById(id);
    }

    @Operation(summary = "更新资源配额", description = "根据配额 ID 更新租户各项资源的上限配置。")
    @PutMapping
    public TenantResourceQuota update(@RequestBody TenantResourceQuota entity) {
        log.info("[API] Update tenant resource quota: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        return tenantResourceQuotaService.updateQuota(entity);
    }

    @Operation(summary = "删除资源配额", description = "根据配额 ID 删除指定的租户资源配额。")
    @DeleteMapping("/{id}")
    public boolean deleteById(@Parameter(description = "配额ID") @PathVariable String id) {
        log.info("[API] Delete tenant resource quota: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return tenantResourceQuotaService.deleteById(id);
    }

    @Operation(summary = "校验资源配额", description = "检查租户在指定资源类型下是否仍有可用配额。")
    @GetMapping("/check")
    public boolean checkQuota(
            @Parameter(description = "租户ID") @RequestParam String tenantId,
            @Parameter(description = "资源类型") @RequestParam String resourceType) {
        log.info("[API] Check tenant quota: tenantId={}, resourceType={}", (tenantId + "").replace("\r", "_").replace("\n", "_"), (resourceType + "").replace("\r", "_").replace("\n", "_"));
        return tenantResourceQuotaService.checkQuota(tenantId, resourceType);
    }

    @Operation(summary = "查询租户配额列表", description = "查询指定租户的全部资源配额记录。")
    @GetMapping("/tenant/{tenantId}")
    public List<TenantResourceQuota> listByTenantId(@Parameter(description = "租户ID") @PathVariable String tenantId) {
        log.info("[API] Query tenant resource quotas: tenantId={}", (tenantId + "").replace("\r", "_").replace("\n", "_"));
        return tenantResourceQuotaService.listByTenantId(tenantId);
    }

    @Operation(summary = "分页查询资源配额", description = "按租户分页查询租户资源配额列表。")
    @GetMapping("/page")
    public Page<TenantResourceQuota> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "租户ID") @RequestParam(required = false) String tenantId) {
        log.info("[API] Page query tenant resource quotas: page={}, size={}", page, size);
        return tenantResourceQuotaService.pageQuery(page, size, tenantId);
    }
}
