package com.scmcloud.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.tenant.domain.entity.TenantFeature;
import com.scmcloud.tenant.service.ITenantFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/tenant-feature")
@Tag(name = "租户功能", description = "提供租户功能开关的维护、状态校验与分页查询接口。")
public class TenantFeatureController {
    private final ITenantFeatureService tenantFeatureService;

    @Operation(summary = "创建租户功能", description = "为指定租户新增一个功能开关记录并返回创建结果。")
    @PostMapping
    public TenantFeature create(@RequestBody TenantFeature entity) {
        log.info("[API] Create tenant feature: tenantId={}, featureCode={}", entity.getTenantId(), entity.getFeatureCode());
        return tenantFeatureService.createFeature(entity);
    }

    @Operation(summary = "查询功能详情", description = "根据功能记录 ID 查询租户功能开关详情。")
    @GetMapping("/{id}")
    public TenantFeature getById(@Parameter(description = "功能记录ID") @PathVariable String id) {
        log.info("[API] Query tenant feature: id={}", id);
        return tenantFeatureService.getById(id);
    }

    @Operation(summary = "更新租户功能", description = "根据功能记录 ID 更新租户功能开关配置。")
    @PutMapping
    public TenantFeature update(@RequestBody TenantFeature entity) {
        log.info("[API] Update tenant feature: id={}", entity.getId());
        return tenantFeatureService.updateFeature(entity);
    }

    @Operation(summary = "删除租户功能", description = "根据功能记录 ID 删除指定的租户功能开关。")
    @DeleteMapping("/{id}")
    public boolean deleteById(@Parameter(description = "功能记录ID") @PathVariable String id) {
        log.info("[API] Delete tenant feature: id={}", id);
        return tenantFeatureService.deleteById(id);
    }

    @Operation(summary = "校验功能是否启用", description = "判断指定租户的某个功能开关当前是否处于启用状态。")
    @GetMapping("/check")
    public boolean isFeatureEnabled(
            @Parameter(description = "租户ID") @RequestParam String tenantId,
            @Parameter(description = "功能编码") @RequestParam String featureCode) {
        log.info("[API] Check if feature is enabled: tenantId={}, featureCode={}", tenantId, featureCode);
        return tenantFeatureService.isFeatureEnabled(tenantId, featureCode);
    }

    @Operation(summary = "查询租户功能列表", description = "查询指定租户下全部功能开关记录。")
    @GetMapping("/tenant/{tenantId}")
    public List<TenantFeature> listByTenantId(@Parameter(description = "租户ID") @PathVariable String tenantId) {
        log.info("[API] Query tenant features list: tenantId={}", tenantId);
        return tenantFeatureService.listByTenantId(tenantId);
    }

    @Operation(summary = "分页查询租户功能", description = "按租户、功能编码和启用状态分页查询租户功能开关。")
    @GetMapping("/page")
    public Page<TenantFeature> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "租户ID") @RequestParam(required = false) String tenantId,
            @Parameter(description = "功能编码") @RequestParam(required = false) String featureCode,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled) {
        log.info("[API] Page query tenant features: page={}, size={}", page, size);
        return tenantFeatureService.pageQuery(page, size, tenantId, featureCode, enabled);
    }
}
