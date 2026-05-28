package scm.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.tenant.domain.entity.TenantResourceQuota;
import scm.tenant.service.impl.TenantResourceQuotaServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tenant-resource-quota")
@Tag(name = "租户资源配额管理", description = "租户资源配额的增删改查接口")
public class TenantResourceQuotaController {

    @Autowired
    private TenantResourceQuotaServiceImpl tenantResourceQuotaService;

    @PostMapping
    @Operation(summary = "创建租户资源配额")
    public TenantResourceQuota create(@RequestBody TenantResourceQuota entity) {
        log.info("[API] 创建租户资源配额: tenantId={}", entity.getTenantId());
        return tenantResourceQuotaService.createQuota(entity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询租户资源配额")
    public TenantResourceQuota getById(@PathVariable String id) {
        log.info("[API] 查询租户资源配额: id={}", id);
        return tenantResourceQuotaService.getById(id);
    }

    @PutMapping
    @Operation(summary = "更新租户资源配额")
    public TenantResourceQuota update(@RequestBody TenantResourceQuota entity) {
        log.info("[API] 更新租户资源配额: id={}", entity.getId());
        return tenantResourceQuotaService.updateQuota(entity);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除租户资源配额")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除租户资源配额: id={}", id);
        return tenantResourceQuotaService.deleteById(id);
    }

    @GetMapping("/check")
    @Operation(summary = "检查租户配额")
    public boolean checkQuota(
            @Parameter(description = "租户ID", required = true) @RequestParam String tenantId,
            @Parameter(description = "资源类型", required = true) @RequestParam String resourceType) {
        log.info("[API] 检查租户配额: tenantId={}, resourceType={}", tenantId, resourceType);
        return tenantResourceQuotaService.checkQuota(tenantId, resourceType);
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "根据租户ID查询资源配额")
    public List<TenantResourceQuota> listByTenantId(@PathVariable String tenantId) {
        log.info("[API] 查询租户资源配额: tenantId={}", tenantId);
        return tenantResourceQuotaService.listByTenantId(tenantId);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询租户资源配额")
    public Page<TenantResourceQuota> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "租户ID") @RequestParam(required = false) String tenantId) {
        log.info("[API] 分页查询租户资源配额: page={}, size={}", page, size);
        return tenantResourceQuotaService.pageQuery(page, size, tenantId);
    }
}
