package com.scmcloud.tenant.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.tenant.domain.entity.TenantResourceQuota;
import com.scmcloud.tenant.mapper.TenantResourceQuotaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scmcloud.tenant.domain.entity.TenantPackage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantResourceQuotaCommandService {
    private final TenantResourceQuotaMapper tenantResourceQuotaMapper;

    @Master(reason = "创建租户资源配额")
    @Transactional(rollbackFor = Exception.class)
    public TenantResourceQuota createQuota(TenantResourceQuota entity) {
        log.info("创建租户资源配额: tenantId={}", entity.getTenantId());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        tenantResourceQuotaMapper.insert(entity);
        log.info("租户资源配额创建成功: id={}", entity.getId());
        return entity;
    }

    @Master(reason = "更新租户资源配额")
    @Transactional(rollbackFor = Exception.class)
    public TenantResourceQuota updateQuota(TenantResourceQuota entity) {
        log.info("更新租户资源配额: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        tenantResourceQuotaMapper.updateById(entity);
        return entity;
    }

    @Master(reason = "删除租户资源配额")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(String id) {
        log.info("删除租户资源配额: id={}", id);
        return tenantResourceQuotaMapper.deleteById(id) > 0;
    }

    @Master(reason = "创建租户默认资源配额")
    @Transactional(rollbackFor = Exception.class)
    public void createDefaults(String tenantId, TenantPackage pkg) {
        log.info("Creating default resource quotas: tenantId={}, package={}", tenantId, pkg.getPackageCode());

        TenantResourceQuota quota = new TenantResourceQuota();
        quota.setId(UUID.randomUUID().toString());
        quota.setTenantId(tenantId);
        quota.setMaxUsers(pkg.getMaxUsers());
        quota.setCurrentUsers(0);
        quota.setMaxWarehouses(pkg.getMaxWarehouses());
        quota.setCurrentWarehouses(0);
        quota.setMaxSkus(pkg.getMaxSkus());
        quota.setCurrentSkus(0);
        quota.setMaxOrdersPerDay(pkg.getMaxOrdersPerDay());
        quota.setCurrentOrdersToday(0);
        quota.setMaxStorageGb(pkg.getMaxStorageGb() != null ? pkg.getMaxStorageGb().intValue() : 10);
        quota.setCurrentStorageGb(BigDecimal.ZERO);
        quota.setMaxApiCallsPerDay(pkg.getMaxApiCallsPerDay() != null ? pkg.getMaxApiCallsPerDay() : 100000);
        quota.setCurrentApiCallsToday(0);
        quota.setCreateTime(LocalDateTime.now());
        quota.setUpdateTime(LocalDateTime.now());

        tenantResourceQuotaMapper.insert(quota);
        log.info("Default resource quotas created: id={}", quota.getId());
    }
}
