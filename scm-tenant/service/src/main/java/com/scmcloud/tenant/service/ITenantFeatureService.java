package com.scmcloud.tenant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.tenant.domain.entity.TenantFeature;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;

/**
 * <p>
 * 绉熸埛鍔熻兘寮€鍏宠〃 鏈嶅姟锟?
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface ITenantFeatureService extends IService<TenantFeature> {

    TenantFeature createFeature(TenantFeature entity);

    TenantFeature getById(String id);

    TenantFeature updateFeature(TenantFeature entity);

    boolean deleteById(String id);

    boolean isFeatureEnabled(String tenantId, String featureCode);

    List<TenantFeature> listByTenantId(String tenantId);

    Page<TenantFeature> pageQuery(int page, int size, String tenantId, String featureCode, Boolean enabled);
}
