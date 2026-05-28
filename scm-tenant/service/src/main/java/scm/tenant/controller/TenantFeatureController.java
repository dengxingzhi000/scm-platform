package scm.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.tenant.domain.entity.TenantFeature;
import scm.tenant.service.impl.TenantFeatureServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tenant-feature")
@Tag(name = "租户功能管理", description = "租户功能开关的增删改查接口")
public class TenantFeatureController {

    @Autowired
    private TenantFeatureServiceImpl tenantFeatureService;

    @PostMapping
    @Operation(summary = "创建租户功能")
    public TenantFeature create(@RequestBody TenantFeature entity) {
        log.info("[API] 创建租户功能: tenantId={}, featureCode={}", entity.getTenantId(), entity.getFeatureCode());
        return tenantFeatureService.createFeature(entity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询租户功能")
    public TenantFeature getById(@PathVariable String id) {
        log.info("[API] 查询租户功能: id={}", id);
        return tenantFeatureService.getById(id);
    }

    @PutMapping
    @Operation(summary = "更新租户功能")
    public TenantFeature update(@RequestBody TenantFeature entity) {
        log.info("[API] 更新租户功能: id={}", entity.getId());
        return tenantFeatureService.updateFeature(entity);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除租户功能")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除租户功能: id={}", id);
        return tenantFeatureService.deleteById(id);
    }

    @GetMapping("/check")
    @Operation(summary = "检查功能是否启用")
    public boolean isFeatureEnabled(
            @Parameter(description = "租户ID", required = true) @RequestParam String tenantId,
            @Parameter(description = "功能代码", required = true) @RequestParam String featureCode) {
        log.info("[API] 检查功能是否启用: tenantId={}, featureCode={}", tenantId, featureCode);
        return tenantFeatureService.isFeatureEnabled(tenantId, featureCode);
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "根据租户ID查询功能列表")
    public List<TenantFeature> listByTenantId(@PathVariable String tenantId) {
        log.info("[API] 查询租户功能列表: tenantId={}", tenantId);
        return tenantFeatureService.listByTenantId(tenantId);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询租户功能")
    public Page<TenantFeature> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "租户ID") @RequestParam(required = false) String tenantId,
            @Parameter(description = "功能代码") @RequestParam(required = false) String featureCode,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled) {
        log.info("[API] 分页查询租户功能: page={}, size={}", page, size);
        return tenantFeatureService.pageQuery(page, size, tenantId, featureCode, enabled);
    }
}
