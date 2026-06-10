package com.scmcloud.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.scmcloud.tenant.domain.entity.TenantResourceQuota;
import com.scmcloud.tenant.mapper.TenantResourceQuotaMapper;
import com.scmcloud.tenant.service.ITenantResourceQuotaService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TenantResourceQuotaServiceImpl extends ServiceImpl<TenantResourceQuotaMapper, TenantResourceQuota> implements ITenantResourceQuotaService {

    public TenantResourceQuota createQuota(TenantResourceQuota entity) {
        log.info("Create tenant resource quota: tenantId={}", entity.getTenantId());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        save(entity);
        log.info("Tenant resource quota created successfully: id={}", entity.getId());
        return entity;
    }

    public TenantResourceQuota getById(String id) {
        return lambdaQuery()
                .eq(TenantResourceQuota::getId, id)
                .one();
    }

    public TenantResourceQuota updateQuota(TenantResourceQuota entity) {
        log.info("Update tenant resource quota: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    public boolean deleteById(String id) {
        log.info("Delete tenant resource quota: id={}", id);
        return removeById(id);
    }

    public boolean checkQuota(String tenantId, String resourceType) {
        log.debug("Check tenant quota: tenantId={}, resourceType={}", tenantId, resourceType);

        TenantResourceQuota quota = lambdaQuery()
                .eq(TenantResourceQuota::getTenantId, tenantId)
                .one();

        if (quota == null) {
            log.warn("Tenant quota not found: tenantId={}", tenantId);
            return false;
        }

        return switch (resourceType) {
            case "USER" -> quota.getCurrentUsers() < quota.getMaxUsers();
            case "WAREHOUSE" -> quota.getCurrentWarehouses() < quota.getMaxWarehouses();
            case "SKU" -> quota.getCurrentSkus() < quota.getMaxSkus();
            case "ORDER" -> quota.getCurrentOrdersToday() < quota.getMaxOrdersPerDay();
            case "API" -> quota.getCurrentApiCallsToday() < quota.getMaxApiCallsPerDay();
            default -> {
                log.warn("鏈煡璧勬簮绫诲瀷: {}", resourceType);
                yield false;
            }
        };
    }

    public List<TenantResourceQuota> listByTenantId(String tenantId) {
        return lambdaQuery()
                .eq(TenantResourceQuota::getTenantId, tenantId)
                .list();
    }

    public Page<TenantResourceQuota> pageQuery(int page, int size, String tenantId) {
        log.debug("Page query tenant resource quota: page={}, size={}, tenantId={}", page, size, tenantId);

        LambdaQueryWrapper<TenantResourceQuota> wrapper = Wrappers.lambdaQuery();

        if (tenantId != null) {
            wrapper.eq(TenantResourceQuota::getTenantId, tenantId);
        }
        wrapper.orderByDesc(TenantResourceQuota::getCreateTime);

        Page<TenantResourceQuota> pageParam = new Page<>(page, size);
        return page(pageParam, wrapper);
    }
}
