package com.scmcloud.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
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
public class TenantResourceQuotaServiceImpl extends ServiceImpl<TenantResourceQuotaMapper, TenantResourceQuota>
        implements ITenantResourceQuotaService {

    @Override
    public TenantResourceQuota createQuota(TenantResourceQuota entity) {
        log.info("Create tenant resource quota: tenantId={}", (entity.getTenantId() + "").replace("\r", "_").replace("\n", "_"));
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        save(entity);
        log.info("Tenant resource quota created successfully: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        return entity;
    }

    @Override
    public TenantResourceQuota getById(String id) {
        return lambdaQuery()
                .eq(TenantResourceQuota::getId, id)
                .one();
    }

    @Override
    public TenantResourceQuota updateQuota(TenantResourceQuota entity) {
        log.info("Update tenant resource quota: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    @Override
    public boolean deleteById(String id) {
        log.info("Delete tenant resource quota: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return removeById(id);
    }

    @Override
    public boolean checkQuota(String tenantId, String resourceType) {
        log.debug("Check tenant quota: tenantId={}, resourceType={}", (tenantId + "").replace("\r", "_").replace("\n", "_"), (resourceType + "").replace("\r", "_").replace("\n", "_"));

        TenantResourceQuota quota = lambdaQuery()
                .eq(TenantResourceQuota::getTenantId, tenantId)
                .one();

        if (quota == null) {
            log.warn("Tenant quota not found: tenantId={}", (tenantId + "").replace("\r", "_").replace("\n", "_"));
            return false;
        }

        return switch (resourceType) {
            case "USER" -> quota.getCurrentUsers() < quota.getMaxUsers();
            case "WAREHOUSE" -> quota.getCurrentWarehouses() < quota.getMaxWarehouses();
            case "SKU" -> quota.getCurrentSkus() < quota.getMaxSkus();
            case "ORDER" -> quota.getCurrentOrdersToday() < quota.getMaxOrdersPerDay();
            case "API" -> quota.getCurrentApiCallsToday() < quota.getMaxApiCallsPerDay();
            default -> {
                log.warn("鏈煡璧勬簮绫诲瀷: {}", (resourceType + "").replace("\r", "_").replace("\n", "_"));
                yield false;
            }
        };
    }

    @Override
    public List<TenantResourceQuota> listByTenantId(String tenantId) {
        return lambdaQuery()
                .eq(TenantResourceQuota::getTenantId, tenantId)
                .list();
    }

    @Override
    public Page<TenantResourceQuota> pageQuery(int page, int size, String tenantId) {
        log.debug("Page query tenant resource quota: page={}, size={}, tenantId={}", (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (tenantId + "").replace("\r", "_").replace("\n", "_"));

        LambdaQueryWrapper<TenantResourceQuota> wrapper = Wrappers.lambdaQuery();

        if (tenantId != null) {
            wrapper.eq(TenantResourceQuota::getTenantId, tenantId);
        }
        wrapper.orderByDesc(TenantResourceQuota::getCreateTime);

        Page<TenantResourceQuota> pageParam = new Page<>(page, size);
        return page(pageParam, wrapper);
    }
}
