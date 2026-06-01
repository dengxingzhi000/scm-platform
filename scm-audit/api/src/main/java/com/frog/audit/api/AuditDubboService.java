package com.frog.audit.api;

import com.frog.audit.api.dto.AuditQueryResult;
import com.frog.audit.api.request.AuditLogRequest;
import com.frog.audit.api.request.AuditQueryRequest;

/**
 * 审计服务 Dubbo 接口
 *
 * <p>提供操作日志记录、日志查询等核心功能，供其他微服务通过 RPC 调用。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface AuditDubboService {

    /**
     * 记录操作日志
     *
     * @param request 审计日志请求
     */
    void logOperation(AuditLogRequest request);

    /**
     * 查询审计日志
     *
     * @param request 查询请求
     * @return 查询结果
     */
    AuditQueryResult queryAuditLogs(AuditQueryRequest request);
}
