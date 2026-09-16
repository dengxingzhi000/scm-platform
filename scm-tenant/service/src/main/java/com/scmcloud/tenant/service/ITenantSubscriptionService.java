package com.scmcloud.tenant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.tenant.domain.entity.TenantSubscription;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;

/**
 * <p>
 * 绉熸埛璁㈤槄锟芥湇鍔★拷
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface ITenantSubscriptionService extends IService<TenantSubscription> {

    TenantSubscription createSubscription(TenantSubscription entity);

    TenantSubscription getById(String id);

    TenantSubscription updateSubscription(TenantSubscription entity);

    boolean deleteById(String id);

    boolean subscribe(String tenantId, String packageId);

    boolean unsubscribe(String id);

    List<TenantSubscription> listByTenantId(String tenantId);

    Page<TenantSubscription> pageQuery(int page, int size, String tenantId, Integer status);
}
