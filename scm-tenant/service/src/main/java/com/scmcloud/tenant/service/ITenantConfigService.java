package com.scmcloud.tenant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.tenant.domain.entity.TenantConfig;

/**
 * <p>
 * 绉熸埛閰嶇疆锟芥湇鍔★拷
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface ITenantConfigService extends IService<TenantConfig> {

    TenantConfig createConfig(TenantConfig entity);

    TenantConfig getById(String id);

    TenantConfig updateConfig(TenantConfig entity);

    boolean deleteById(String id);

    TenantConfig getConfigByTenantAndKey(String tenantId, String configKey);

    Page<TenantConfig> pageQuery(int page, int size, String tenantId, String configCategory, String configKey);
}
