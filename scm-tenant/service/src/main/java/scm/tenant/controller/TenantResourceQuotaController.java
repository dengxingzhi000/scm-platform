package scm.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.tenant.domain.entity.TenantResourceQuota;
import scm.tenant.service.impl.TenantResourceQuotaServiceImpl;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/tenant-resource-quota")
public class TenantResourceQuotaController {
    private final TenantResourceQuotaServiceImpl tenantResourceQuotaService;
    @PostMapping
    public TenantResourceQuota create(@RequestBody TenantResourceQuota entity) {
        log.info("[API] 创建租户资源配额: tenantId={}", entity.getTenantId());
        return tenantResourceQuotaService.createQuota(entity);
    }

    @GetMapping("/{id}")
    public TenantResourceQuota getById(@PathVariable String id) {
        log.info("[API] 查询租户资源配额: id={}", id);
        return tenantResourceQuotaService.getById(id);
    }

    @PutMapping
    public TenantResourceQuota update(@RequestBody TenantResourceQuota entity) {
        log.info("[API] 更新租户资源配额: id={}", entity.getId());
        return tenantResourceQuotaService.updateQuota(entity);
    }

    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除租户资源配额: id={}", id);
        return tenantResourceQuotaService.deleteById(id);
    }

    @GetMapping("/check")
    public boolean checkQuota(
            @RequestParam String tenantId,
            @RequestParam String resourceType) {
        log.info("[API] 检查租户配�? tenantId={}, resourceType={}", tenantId, resourceType);
        return tenantResourceQuotaService.checkQuota(tenantId, resourceType);
    }

    @GetMapping("/tenant/{tenantId}")
    public List<TenantResourceQuota> listByTenantId(@PathVariable String tenantId) {
        log.info("[API] 查询租户资源配额: tenantId={}", tenantId);
        return tenantResourceQuotaService.listByTenantId(tenantId);
    }

    @GetMapping("/page")
    public Page<TenantResourceQuota> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tenantId) {
        log.info("[API] 分页查询租户资源配额: page={}, size={}", page, size);
        return tenantResourceQuotaService.pageQuery(page, size, tenantId);
    }
}
