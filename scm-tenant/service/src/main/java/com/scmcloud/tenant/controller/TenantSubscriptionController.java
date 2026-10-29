package com.scmcloud.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.tenant.domain.entity.TenantSubscription;
import com.scmcloud.tenant.service.ITenantSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/tenant-subscription")
@Tag(name = "租户订阅", description = "提供租户套餐订阅的创建、开通、取消与分页查询接口。")
public class TenantSubscriptionController {
    private final ITenantSubscriptionService tenantSubscriptionService;

    @Operation(summary = "创建租户订阅", description = "新增一条租户套餐订阅记录并返回创建结果。")
    @PostMapping
    public TenantSubscription create(@RequestBody TenantSubscription entity) {
        log.info("[API] 创建租户订阅: tenantId={}, packageId={}", (entity.getTenantId() + "").replace("\r", "_").replace("\n", "_"), (entity.getPackageId() + "").replace("\r", "_").replace("\n", "_"));
        return tenantSubscriptionService.createSubscription(entity);
    }

    @Operation(summary = "查询订阅详情", description = "根据订阅 ID 查询租户订阅详情。")
    @GetMapping("/{id}")
    public TenantSubscription getById(@Parameter(description = "订阅ID") @PathVariable String id) {
        log.info("[API] 查询租户订阅: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return tenantSubscriptionService.getById(id);
    }

    @Operation(summary = "更新租户订阅", description = "根据订阅 ID 更新订阅套餐、有效期等订阅信息。")
    @PutMapping
    public TenantSubscription update(@RequestBody TenantSubscription entity) {
        log.info("[API] 更新租户订阅: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        return tenantSubscriptionService.updateSubscription(entity);
    }

    @Operation(summary = "删除租户订阅", description = "根据订阅 ID 删除指定的租户订阅记录。")
    @DeleteMapping("/{id}")
    public boolean deleteById(@Parameter(description = "订阅ID") @PathVariable String id) {
        log.info("[API] 删除租户订阅: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return tenantSubscriptionService.deleteById(id);
    }

    @Operation(summary = "订阅租户套餐", description = "为指定租户开通指定套餐的订阅。")
    @PostMapping("/subscribe")
    public boolean subscribe(
            @Parameter(description = "租户ID") @RequestParam String tenantId,
            @Parameter(description = "套餐ID") @RequestParam String packageId) {
        log.info("[API] 租户订阅: tenantId={}, packageId={}", (tenantId + "").replace("\r", "_").replace("\n", "_"), (packageId + "").replace("\r", "_").replace("\n", "_"));
        return tenantSubscriptionService.subscribe(tenantId, packageId);
    }

    @Operation(summary = "取消租户订阅", description = "根据订阅 ID 取消指定租户的套餐订阅。")
    @PostMapping("/{id}/unsubscribe")
    public boolean unsubscribe(@Parameter(description = "订阅ID") @PathVariable String id) {
        log.info("[API] 取消租户订阅: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return tenantSubscriptionService.unsubscribe(id);
    }

    @Operation(summary = "查询租户订阅列表", description = "查询指定租户的全部订阅记录。")
    @GetMapping("/tenant/{tenantId}")
    public List<TenantSubscription> listByTenantId(@Parameter(description = "租户ID") @PathVariable String tenantId) {
        log.info("[API] 查询租户订阅: tenantId={}", (tenantId + "").replace("\r", "_").replace("\n", "_"));
        return tenantSubscriptionService.listByTenantId(tenantId);
    }

    @Operation(summary = "分页查询租户订阅", description = "按租户和订阅状态分页查询租户订阅列表。")
    @GetMapping("/page")
    public Page<TenantSubscription> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "租户ID") @RequestParam(required = false) String tenantId,
            @Parameter(description = "订阅状态") @RequestParam(required = false) Integer status) {
        log.info("[API] 分页查询租户订阅: page={}, size={}", page, size);
        return tenantSubscriptionService.pageQuery(page, size, tenantId, status);
    }
}
