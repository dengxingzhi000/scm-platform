package com.scmcloud.tenant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.tenant.domain.entity.Tenant;

import java.util.List;

/**
 * <p>
 * 绉熸埛锟芥湇鍔★拷
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface ITenantService extends IService<Tenant> {

    Tenant createTenant(Tenant entity);

    Tenant getById(String id);

    Tenant updateTenant(Tenant entity);

    boolean deleteById(String id);

    boolean enableTenant(String id);

    boolean disableTenant(String id);

    List<Tenant> listActive();

    Page<Tenant> pageQuery(int page, int size, String tenantName, Integer tenantType, Integer status);
}
