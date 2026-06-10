package com.scmcloud.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.tenant.domain.entity.TenantFeature;
import com.scmcloud.tenant.service.ITenantFeatureService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/tenant-feature")
public class TenantFeatureController {
    private final ITenantFeatureService tenantFeatureService;

    @PostMapping
    public TenantFeature create(@RequestBody TenantFeature entity) {
        log.info("[API] Create tenant feature: tenantId={}, featureCode={}", entity.getTenantId(), entity.getFeatureCode());
        return tenantFeatureService.createFeature(entity);
    }

    @GetMapping("/{id}")
    public TenantFeature getById(@PathVariable String id) {
        log.info("[API] Query tenant feature: id={}", id);
        return tenantFeatureService.getById(id);
    }

    @PutMapping
    public TenantFeature update(@RequestBody TenantFeature entity) {
        log.info("[API] Update tenant feature: id={}", entity.getId());
        return tenantFeatureService.updateFeature(entity);
    }

    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] Delete tenant feature: id={}", id);
        return tenantFeatureService.deleteById(id);
    }

    @GetMapping("/check")
    public boolean isFeatureEnabled(
            @RequestParam String tenantId,
            @RequestParam String featureCode) {
        log.info("[API] Check if feature is enabled: tenantId={}, featureCode={}", tenantId, featureCode);
        return tenantFeatureService.isFeatureEnabled(tenantId, featureCode);
    }

    @GetMapping("/tenant/{tenantId}")
    public List<TenantFeature> listByTenantId(@PathVariable String tenantId) {
        log.info("[API] Query tenant features list: tenantId={}", tenantId);
        return tenantFeatureService.listByTenantId(tenantId);
    }

    @GetMapping("/page")
    public Page<TenantFeature> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String featureCode,
            @RequestParam(required = false) Boolean enabled) {
        log.info("[API] Page query tenant features: page={}, size={}", page, size);
        return tenantFeatureService.pageQuery(page, size, tenantId, featureCode, enabled);
    }
}
