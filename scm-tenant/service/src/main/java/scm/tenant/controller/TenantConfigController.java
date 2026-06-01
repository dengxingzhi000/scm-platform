package scm.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.tenant.domain.entity.TenantConfig;
import scm.tenant.service.impl.TenantConfigServiceImpl;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/tenant-config")
public class TenantConfigController {

    private final TenantConfigServiceImpl tenantConfigService;

    @PostMapping
    public TenantConfig create(@RequestBody TenantConfig entity) {
        log.info("[API] 创建租户配置: tenantId={}, configKey={}", entity.getTenantId(), entity.getConfigKey());
        return tenantConfigService.createConfig(entity);
    }

    @GetMapping("/{id}")
    public TenantConfig getById(@PathVariable String id) {
        log.info("[API] 查询租户配置: id={}", id);
        return tenantConfigService.getById(id);
    }

    @PutMapping
    public TenantConfig update(@RequestBody TenantConfig entity) {
        log.info("[API] 更新租户配置: id={}", entity.getId());
        return tenantConfigService.updateConfig(entity);
    }

    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除租户配置: id={}", id);
        return tenantConfigService.deleteById(id);
    }

    @GetMapping("/tenant/{tenantId}/key/{configKey}")
    public TenantConfig getConfigByTenantAndKey(
            @PathVariable String tenantId,
            @PathVariable String configKey) {
        log.info("[API] 查询租户配置: tenantId={}, configKey={}", tenantId, configKey);
        return tenantConfigService.getConfigByTenantAndKey(tenantId, configKey);
    }

    @GetMapping("/page")
    public Page<TenantConfig> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String configCategory,
            @RequestParam(required = false) String configKey) {
        log.info("[API] 分页查询租户配置: page={}, size={}", page, size);
        return tenantConfigService.pageQuery(page, size, tenantId, configCategory, configKey);
    }
}
