package com.scmcloud.audit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.audit.domain.entity.SysAuditLog;

import java.util.List;

/**
 * <p>
 * 操作审计日志(按月分区) 服务接口
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
public interface ISysAuditLogService extends IService<SysAuditLog> {

    SysAuditLog createLog(SysAuditLog entity);

    SysAuditLog getById(String id);

    boolean deleteById(String id);

    List<SysAuditLog> listByUserId(String userId);

    List<SysAuditLog> listByModule(String module);

    List<SysAuditLog> listByBusinessType(String businessType);

    Page<SysAuditLog> pageQuery(int page, int size, String userId, String operationType,
                                String module, String businessType, Integer riskLevel);
}
