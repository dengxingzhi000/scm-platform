package com.scmcloud.tenant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.tenant.domain.entity.TenantPackage;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;

/**
 * <p>
 * 绉熸埛濂楅锟芥湇鍔★拷
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface ITenantPackageService extends IService<TenantPackage> {

    TenantPackage createPackage(TenantPackage entity);

    TenantPackage getById(String id);

    TenantPackage updatePackage(TenantPackage entity);

    boolean deleteById(String id);

    List<TenantPackage> listActive();

    Page<TenantPackage> pageQuery(int page, int size, String packageName, Integer packageLevel, Boolean enabled);
}
