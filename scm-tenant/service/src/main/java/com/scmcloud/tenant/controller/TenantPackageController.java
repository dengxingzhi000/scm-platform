package com.scmcloud.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.tenant.domain.entity.TenantPackage;
import com.scmcloud.tenant.service.ITenantPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/tenant-package")
@Tag(name = "租户套餐", description = "提供租户套餐的维护、启用套餐查询与分页查询接口。")
public class TenantPackageController {
    private final ITenantPackageService tenantPackageService;

    @Operation(summary = "创建租户套餐", description = "新增一个租户套餐并返回创建后的套餐信息。")
    @PostMapping
    public TenantPackage create(@RequestBody TenantPackage entity) {
        log.info("[API] 创建租户套餐: packageCode={}, packageName={}", entity.getPackageCode(), entity.getPackageName());
        return tenantPackageService.createPackage(entity);
    }

    @Operation(summary = "查询套餐详情", description = "根据套餐 ID 查询租户套餐详情。")
    @GetMapping("/{id}")
    public TenantPackage getById(@Parameter(description = "套餐ID") @PathVariable String id) {
        log.info("[API] 查询租户套餐: id={}", id);
        return tenantPackageService.getById(id);
    }

    @Operation(summary = "更新租户套餐", description = "根据套餐 ID 更新套餐配置与资源上限。")
    @PutMapping
    public TenantPackage update(@RequestBody TenantPackage entity) {
        log.info("[API] 更新租户套餐: id={}", entity.getId());
        return tenantPackageService.updatePackage(entity);
    }

    @Operation(summary = "删除租户套餐", description = "根据套餐 ID 删除指定租户套餐。")
    @DeleteMapping("/{id}")
    public boolean deleteById(@Parameter(description = "套餐ID") @PathVariable String id) {
        log.info("[API] 删除租户套餐: id={}", id);
        return tenantPackageService.deleteById(id);
    }

    @Operation(summary = "查询已启用套餐", description = "查询当前处于启用状态的全部租户套餐。")
    @GetMapping("/active")
    public List<TenantPackage> listActive() {
        return tenantPackageService.listActive();
    }

    @Operation(summary = "分页查询租户套餐", description = "按套餐名称、套餐等级和启用状态分页查询租户套餐。")
    @GetMapping("/page")
    public Page<TenantPackage> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "套餐名称") @RequestParam(required = false) String packageName,
            @Parameter(description = "套餐等级") @RequestParam(required = false) Integer packageLevel,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled) {
        log.info("[API] 分页查询租户套餐: page={}, size={}", page, size);
        return tenantPackageService.pageQuery(page, size, packageName, packageLevel, enabled);
    }
}
