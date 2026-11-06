package com.cognora.platform.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.cognora.platform.tenant.domain.entity.TenantConfig;
import com.cognora.platform.tenant.service.ITenantConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/tenant-config")
@Tag(name = "租户配置", description = "提供租户级配置项的新增、查询、更新、删除与分页查询接口。")
public class TenantConfigController {

    private final ITenantConfigService tenantConfigService;

    @Operation(summary = "创建租户配置", description = "新增一条租户配置项并返回创建后的配置内容。")
    @PostMapping
    public TenantConfig create(@RequestBody TenantConfig entity) {
        log.info("[API] 创建租户配置: tenantId={}, configKey={}", (entity.getTenantId() + "").replace("\r", "_").replace("\n", "_"), (entity.getConfigKey() + "").replace("\r", "_").replace("\n", "_"));
        return tenantConfigService.createConfig(entity);
    }

    @Operation(summary = "查询配置详情", description = "根据配置 ID 查询租户配置详情。")
    @GetMapping("/{id}")
    public TenantConfig getById(@Parameter(description = "配置ID") @PathVariable String id) {
        log.info("[API] 查询租户配置: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return tenantConfigService.getById(id);
    }

    @Operation(summary = "更新租户配置", description = "根据配置 ID 更新租户配置内容并返回更新结果。")
    @PutMapping
    public TenantConfig update(@RequestBody TenantConfig entity) {
        log.info("[API] 更新租户配置: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        return tenantConfigService.updateConfig(entity);
    }

    @Operation(summary = "删除租户配置", description = "根据配置 ID 删除指定租户配置。")
    @DeleteMapping("/{id}")
    public boolean deleteById(@Parameter(description = "配置ID") @PathVariable String id) {
        log.info("[API] 删除租户配置: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return tenantConfigService.deleteById(id);
    }

    @Operation(summary = "按租户与键查询配置", description = "根据租户 ID 和配置键查询唯一的配置项。")
    @GetMapping("/tenant/{tenantId}/key/{configKey}")
    public TenantConfig getConfigByTenantAndKey(
            @Parameter(description = "租户ID") @PathVariable String tenantId,
            @Parameter(description = "配置键") @PathVariable String configKey) {
        log.info("[API] 查询租户配置: tenantId={}, configKey={}", (tenantId + "").replace("\r", "_").replace("\n", "_"), (configKey + "").replace("\r", "_").replace("\n", "_"));
        return tenantConfigService.getConfigByTenantAndKey(tenantId, configKey);
    }

    @Operation(summary = "分页查询租户配置",
            description = "scope=TENANT（默认）时仅返回当前租户的配置；scope=PLATFORM 时仅返回平台级配置"
                    + "（tenant_id 为空），且调用方必须是平台管理员，否则返回 403。")
    @GetMapping("/page")
    public Page<TenantConfig> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "租户ID（scope=TENANT 时忽略，始终过滤当前租户）")
            @RequestParam(required = false) String tenantId,
            @Parameter(description = "配置分类") @RequestParam(required = false) String configCategory,
            @Parameter(description = "配置键") @RequestParam(required = false) String configKey,
            @Parameter(description = "范围：TENANT（默认）/ PLATFORM")
            @RequestParam(required = false, defaultValue = "TENANT") String scope) {
        log.info("[API] 分页查询租户配置: page={}, size={}, scope={}", page, size, scope);
        return tenantConfigService.pageQuery(page, size, tenantId, configCategory, configKey, scope);
    }
}
