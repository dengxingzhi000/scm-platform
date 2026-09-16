package com.scmcloud.audit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.audit.domain.entity.SysSensitiveOperationLog;

import java.util.List;

/**
 * <p>
 * 敏感操作日志 服务接口
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
public interface ISysSensitiveOperationLogService extends IService<SysSensitiveOperationLog> {

    SysSensitiveOperationLog createLog(SysSensitiveOperationLog entity);

    SysSensitiveOperationLog getById(String id);

    boolean deleteById(String id);

    List<SysSensitiveOperationLog> listByUserId(String userId);

    List<SysSensitiveOperationLog> listByRiskLevel(Integer riskScore);

    Page<SysSensitiveOperationLog> pageQuery(int page, int size, String userId,
                                              String operationType, Integer riskScore);
}
