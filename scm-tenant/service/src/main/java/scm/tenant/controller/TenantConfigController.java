package scm.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.tenant.domain.entity.TenantConfig;
import scm.tenant.service.impl.TenantConfigServiceImpl;

@Slf4j
@RestController
@RequestMapping("/tenant-config")
@Tag(name = "租户配置管理", description = "租户配置的增删改查接口")
public class TenantConfigController {

    @Autowired
    private TenantConfigServiceImpl tenantConfigService;

    @PostMapping
    @Operation(summary = "创建租户配置")
    public TenantConfig create(@RequestBody TenantConfig entity) {
        log.info("[API] 创建租户配置: tenantId={}, configKey={}", entity.getTenantId(), entity.getConfigKey());
        return tenantConfigService.createConfig(entity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询租户配置")
    public TenantConfig getById(@PathVariable String id) {
        log.info("[API] 查询租户配置: id={}", id);
        return tenantConfigService.getById(id);
    }

    @PutMapping
    @Operation(summary = "更新租户配置")
    public TenantConfig update(@RequestBody TenantConfig entity) {
        log.info("[API] 更新租户配置: id={}", entity.getId());
        return tenantConfigService.updateConfig(entity);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除租户配置")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除租户配置: id={}", id);
        return tenantConfigService.deleteById(id);
    }

    @GetMapping("/tenant/{tenantId}/key/{configKey}")
    @Operation(summary = "根据租户ID和配置键查询配置")
    public TenantConfig getConfigByTenantAndKey(
            @PathVariable String tenantId,
            @PathVariable String configKey) {
        log.info("[API] 查询租户配置: tenantId={}, configKey={}", tenantId, configKey);
        return tenantConfigService.getConfigByTenantAndKey(tenantId, configKey);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询租户配置")
    public Page<TenantConfig> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "租户ID") @RequestParam(required = false) String tenantId,
            @Parameter(description = "配置分类") @RequestParam(required = false) String configCategory,
            @Parameter(description = "配置键") @RequestParam(required = false) String configKey) {
        log.info("[API] 分页查询租户配置: page={}, size={}", page, size);
        return tenantConfigService.pageQuery(page, size, tenantId, configCategory, configKey);
    }
}
