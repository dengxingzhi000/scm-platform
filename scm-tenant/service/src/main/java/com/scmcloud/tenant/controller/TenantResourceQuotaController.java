package com.scmcloud.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.tenant.domain.entity.TenantResourceQuota;
import com.scmcloud.tenant.service.ITenantResourceQuotaService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/tenant-resource-quota")
public class TenantResourceQuotaController {
    private final ITenantResourceQuotaService tenantResourceQuotaService;
    @PostMapping
    public TenantResourceQuota create(@RequestBody TenantResourceQuota entity) {
        log.info("[API] Create tenant resource quota: tenantId={}", entity.getTenantId());
        return tenantResourceQuotaService.createQuota(entity);
    }

    @GetMapping("/{id}")
    public TenantResourceQuota getById(@PathVariable String id) {
        log.info("[API] Query tenant resource quota: id={}", id);
        return tenantResourceQuotaService.getById(id);
    }

    @PutMapping
    public TenantResourceQuota update(@RequestBody TenantResourceQuota entity) {
        log.info("[API] Update tenant resource quota: id={}", entity.getId());
        return tenantResourceQuotaService.updateQuota(entity);
    }

    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] Delete tenant resource quota: id={}", id);
        return tenantResourceQuotaService.deleteById(id);
    }

    @GetMapping("/check")
    public boolean checkQuota(
            @RequestParam String tenantId,
            @RequestParam String resourceType) {
        log.info("[API] Check tenant quota: tenantId={}, resourceType={}", tenantId, resourceType);
        return tenantResourceQuotaService.checkQuota(tenantId, resourceType);
    }

    @GetMapping("/tenant/{tenantId}")
    public List<TenantResourceQuota> listByTenantId(@PathVariable String tenantId) {
        log.info("[API] Query tenant resource quotas: tenantId={}", tenantId);
        return tenantResourceQuotaService.listByTenantId(tenantId);
    }

    @GetMapping("/page")
    public Page<TenantResourceQuota> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tenantId) {
        log.info("[API] Page query tenant resource quotas: page={}, size={}", page, size);
        return tenantResourceQuotaService.pageQuery(page, size, tenantId);
    }
}
