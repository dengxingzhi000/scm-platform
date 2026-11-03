package com.scmcloud.tenant.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.tenant.domain.entity.TenantFeature;
import com.scmcloud.tenant.mapper.TenantFeatureMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantFeatureQueryService {

    private final TenantFeatureMapper tenantFeatureMapper;

    @Slave
    public TenantFeature getById(String id) {
        LambdaQueryWrapper<TenantFeature> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TenantFeature::getId, id);
        return tenantFeatureMapper.selectOne(wrapper);
    }

    @Slave
    public boolean isFeatureEnabled(String tenantId, String featureCode) {
        LambdaQueryWrapper<TenantFeature> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TenantFeature::getTenantId, tenantId)
                .eq(TenantFeature::getFeatureCode, featureCode);
        TenantFeature feature = tenantFeatureMapper.selectOne(wrapper);
        if (feature == null) {
            return false;
        }
        return Boolean.TRUE.equals(feature.getEnabled());
    }

    @Slave
    public List<TenantFeature> listByTenantId(String tenantId) {
        LambdaQueryWrapper<TenantFeature> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TenantFeature::getTenantId, tenantId)
                .orderByAsc(TenantFeature::getFeatureCode);
        return tenantFeatureMapper.selectList(wrapper);
    }

    @Slave
    public Page<TenantFeature> pageQuery(int page, int size, String tenantId, String featureCode, Boolean enabled) {
        LambdaQueryWrapper<TenantFeature> wrapper = Wrappers.lambdaQuery();

        if (tenantId != null) {
            wrapper.eq(TenantFeature::getTenantId, tenantId);
        }
        if (featureCode != null) {
            wrapper.eq(TenantFeature::getFeatureCode, featureCode);
        }
        if (enabled != null) {
            wrapper.eq(TenantFeature::getEnabled, enabled);
        }
        wrapper.orderByAsc(TenantFeature::getFeatureCode);

        return tenantFeatureMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
