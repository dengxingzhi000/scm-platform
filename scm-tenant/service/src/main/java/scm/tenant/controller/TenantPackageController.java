package scm.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.tenant.domain.entity.TenantPackage;
import scm.tenant.service.impl.TenantPackageServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tenant-package")
@Tag(name = "租户套餐管理", description = "租户套餐的增删改查接口")
public class TenantPackageController {

    @Autowired
    private TenantPackageServiceImpl tenantPackageService;

    @PostMapping
    @Operation(summary = "创建租户套餐")
    public TenantPackage create(@RequestBody TenantPackage entity) {
        log.info("[API] 创建租户套餐: packageCode={}, packageName={}", entity.getPackageCode(), entity.getPackageName());
        return tenantPackageService.createPackage(entity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询租户套餐")
    public TenantPackage getById(@PathVariable String id) {
        log.info("[API] 查询租户套餐: id={}", id);
        return tenantPackageService.getById(id);
    }

    @PutMapping
    @Operation(summary = "更新租户套餐")
    public TenantPackage update(@RequestBody TenantPackage entity) {
        log.info("[API] 更新租户套餐: id={}", entity.getId());
        return tenantPackageService.updatePackage(entity);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除租户套餐")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除租户套餐: id={}", id);
        return tenantPackageService.deleteById(id);
    }

    @GetMapping("/active")
    @Operation(summary = "查询启用的租户套餐")
    public List<TenantPackage> listActive() {
        log.info("[API] 查询启用的租户套餐");
        return tenantPackageService.listActive();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询租户套餐")
    public Page<TenantPackage> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "套餐名称") @RequestParam(required = false) String packageName,
            @Parameter(description = "套餐级别") @RequestParam(required = false) Integer packageLevel,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled) {
        log.info("[API] 分页查询租户套餐: page={}, size={}", page, size);
        return tenantPackageService.pageQuery(page, size, packageName, packageLevel, enabled);
    }
}
