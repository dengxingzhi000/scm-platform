package com.scmcloud.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.scmcloud.tenant.domain.entity.TenantFeature;
import com.scmcloud.tenant.mapper.TenantFeatureMapper;
import com.scmcloud.tenant.service.ITenantFeatureService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TenantFeatureServiceImpl extends ServiceImpl<TenantFeatureMapper, TenantFeature> implements ITenantFeatureService {

    @Override
    public TenantFeature createFeature(TenantFeature entity) {
        log.info("Create tenant feature: tenantId={}, featureCode={}", (entity.getTenantId() + "").replace("\r", "_").replace("\n", "_"), (entity.getFeatureCode() + "").replace("\r", "_").replace("\n", "_"));
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getEnabled() == null) {
            entity.setEnabled(true);
        }
        save(entity);
        log.info("Tenant feature created successfully: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        return entity;
    }

    @Override
    public TenantFeature getById(String id) {
        return lambdaQuery()
                .eq(TenantFeature::getId, id)
                .one();
    }

    @Override
    public TenantFeature updateFeature(TenantFeature entity) {
        log.info("Update tenant feature: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    @Override
    public boolean deleteById(String id) {
        log.info("Delete tenant feature: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return removeById(id);
    }

    @Override
    public boolean isFeatureEnabled(String tenantId, String featureCode) {
        log.debug("Check if feature is enabled: tenantId={}, featureCode={}", (tenantId + "").replace("\r", "_").replace("\n", "_"), (featureCode + "").replace("\r", "_").replace("\n", "_"));

        TenantFeature feature = lambdaQuery()
                .eq(TenantFeature::getTenantId, tenantId)
                .eq(TenantFeature::getFeatureCode, featureCode)
                .one();

        if (feature == null) {
            log.debug("Feature not found: tenantId={}, featureCode={}", (tenantId + "").replace("\r", "_").replace("\n", "_"), (featureCode + "").replace("\r", "_").replace("\n", "_"));
            return false;
        }

        boolean enabled = Boolean.TRUE.equals(feature.getEnabled());
        log.debug("Feature check result: tenantId={}, featureCode={}, enabled={}", (tenantId + "").replace("\r", "_").replace("\n", "_"), (featureCode + "").replace("\r", "_").replace("\n", "_"), enabled);
        return enabled;
    }

    @Override
    public List<TenantFeature> listByTenantId(String tenantId) {
        return lambdaQuery()
                .eq(TenantFeature::getTenantId, tenantId)
                .orderByAsc(TenantFeature::getFeatureCode)
                .list();
    }

    @Override
    public Page<TenantFeature> pageQuery(int page, int size, String tenantId, String featureCode, Boolean enabled) {
        log.debug("Page query tenant features: page={}, size={}, tenantId={}, featureCode={}, enabled={}",
                page, size, (tenantId + "").replace("\r", "_").replace("\n", "_"), (featureCode + "").replace("\r", "_").replace("\n", "_"), enabled);

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

        Page<TenantFeature> pageParam = new Page<>(page, size);
        return page(pageParam, wrapper);
    }
}
