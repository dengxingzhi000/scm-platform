package com.scmcloud.tenant.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.tenant.domain.entity.TenantResourceQuota;
import com.scmcloud.tenant.mapper.TenantResourceQuotaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantResourceQuotaQueryService {

    private final TenantResourceQuotaMapper tenantResourceQuotaMapper;

    @Slave
    public TenantResourceQuota getById(String id) {
        LambdaQueryWrapper<TenantResourceQuota> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TenantResourceQuota::getId, id);
        return tenantResourceQuotaMapper.selectOne(wrapper);
    }

    @Slave
    public boolean checkQuota(String tenantId, String resourceType) {
        LambdaQueryWrapper<TenantResourceQuota> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TenantResourceQuota::getTenantId, tenantId);
        TenantResourceQuota quota = tenantResourceQuotaMapper.selectOne(wrapper);

        if (quota == null) {
            log.warn("租户配额不存在: tenantId={}", tenantId);
            return false;
        }

        return switch (resourceType) {
            case "USER" -> quota.getCurrentUsers() < quota.getMaxUsers();
            case "WAREHOUSE" -> quota.getCurrentWarehouses() < quota.getMaxWarehouses();
            case "SKU" -> quota.getCurrentSkus() < quota.getMaxSkus();
            case "ORDER" -> quota.getCurrentOrdersToday() < quota.getMaxOrdersPerDay();
            case "API" -> quota.getCurrentApiCallsToday() < quota.getMaxApiCallsPerDay();
            default -> {
                log.warn("未知资源类型: {}", resourceType);
                yield false;
            }
        };
    }

    @Slave
    public List<TenantResourceQuota> listByTenantId(String tenantId) {
        LambdaQueryWrapper<TenantResourceQuota> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TenantResourceQuota::getTenantId, tenantId);
        return tenantResourceQuotaMapper.selectList(wrapper);
    }

    @Slave
    public Page<TenantResourceQuota> pageQuery(int page, int size, String tenantId) {
        LambdaQueryWrapper<TenantResourceQuota> wrapper = Wrappers.lambdaQuery();

        if (tenantId != null) {
            wrapper.eq(TenantResourceQuota::getTenantId, tenantId);
        }
        wrapper.orderByDesc(TenantResourceQuota::getCreateTime);

        return tenantResourceQuotaMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
