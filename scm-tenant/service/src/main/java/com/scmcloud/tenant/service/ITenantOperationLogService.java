package com.scmcloud.tenant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.tenant.domain.entity.TenantOperationLog;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;

/**
 * <p>
 * 绉熸埛鎿嶄綔鏃ュ織锟芥湇鍔★拷 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface ITenantOperationLogService extends IService<TenantOperationLog> {

    TenantOperationLog createLog(TenantOperationLog entity);

    TenantOperationLog getById(String id);

    boolean deleteById(String id);

    List<TenantOperationLog> listByTenantId(String tenantId);

    List<TenantOperationLog> listByOperationType(String operationType);

    Page<TenantOperationLog> pageQuery(int page, int size, String tenantId, String operationType,
                                       String operationModule);
}
