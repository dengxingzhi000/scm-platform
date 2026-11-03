package com.cognora.platform.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.platform.common.log.service.ISysAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.cognora.platform.audit.domain.entity.SysAuditLog;
import com.cognora.platform.audit.domain.entity.SysSensitiveOperationLog;
import com.cognora.platform.audit.mapper.SysAuditLogMapper;
import com.cognora.platform.audit.service.command.SysSensitiveOperationLogCommandService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysAuditLogServiceImpl
        extends ServiceImpl<SysAuditLogMapper, SysAuditLog>
        implements ISysAuditLogService, com.cognora.platform.audit.service.ISysAuditLogService {

    /** Matches {@code @AuditLog.riskLevel} 3/4: password resets, grants, deletes, force logout. */
    static final int SENSITIVE_RISK_THRESHOLD = 3;

    private final SysSensitiveOperationLogCommandService sensitiveOperationLogCommandService;

    @Override
    public SysAuditLog createLog(SysAuditLog entity) {
        log.info("创建审计日志: userId={}, operationType={}, module={}",
                (entity.getUserId() + "").replace("\r", "_").replace("\n", "_"), (entity.getOperationType() + "").replace("\r", "_").replace("\n", "_"), (entity.getOperationModule() + "").replace("\r", "_").replace("\n", "_"));
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        save(entity);
        log.info("审计日志创建成功: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        return entity;
    }

    @Override
    public SysAuditLog getById(String id) {
        return lambdaQuery()
                .eq(SysAuditLog::getId, id)
                .one();
    }

    @Override
    public boolean deleteById(String id) {
        log.info("删除审计日志: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return removeById(id);
    }

    @Override
    public List<SysAuditLog> listByUserId(String userId) {
        return lambdaQuery()
                .eq(SysAuditLog::getUserId, userId)
                .orderByDesc(SysAuditLog::getCreateTime)
                .list();
    }

    @Override
    public List<SysAuditLog> listByModule(String module) {
        return lambdaQuery()
                .eq(SysAuditLog::getOperationModule, module)
                .orderByDesc(SysAuditLog::getCreateTime)
                .list();
    }

    @Override
    public List<SysAuditLog> listByBusinessType(String businessType) {
        return lambdaQuery()
                .eq(SysAuditLog::getBusinessType, businessType)
                .orderByDesc(SysAuditLog::getCreateTime)
                .list();
    }

    @Override
    public Page<SysAuditLog> pageQuery(int page, int size, String userId, String operationType,
                                        String module, String businessType, Integer riskLevel) {
        log.debug("分页查询审计日志: page={}, size={}, userId={}, operationType={}, module={}, businessType={}, riskLevel={}",
                (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (userId + "").replace("\r", "_").replace("\n", "_"), (operationType + "").replace("\r", "_").replace("\n", "_"), (module + "").replace("\r", "_").replace("\n", "_"), (businessType + "").replace("\r", "_").replace("\n", "_"), (riskLevel + "").replace("\r", "_").replace("\n", "_"));

        LambdaQueryWrapper<SysAuditLog> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(userId)) {
            wrapper.eq(SysAuditLog::getUserId, userId);
        }
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(SysAuditLog::getOperationType, operationType);
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(SysAuditLog::getOperationModule, module);
        }
        if (StringUtils.hasText(businessType)) {
            wrapper.eq(SysAuditLog::getBusinessType, businessType);
        }
        if (riskLevel != null) {
            wrapper.eq(SysAuditLog::getRiskLevel, riskLevel);
        }
        wrapper.orderByDesc(SysAuditLog::getCreateTime);

        Page<SysAuditLog> pageParam = new Page<>(page, size);
        return page(pageParam, wrapper);
    }

    // --- Common ISysAuditLogService methods ---

    @Async
    @Override
    public void recordLogin(UUID userId, String username, String ipAddress, boolean success, String remark) {
        SysAuditLog log = SysAuditLog.builder()
                .userId(userId != null ? userId.toString() : null)
                .username(username)
                .operationType("LOGIN")
                .ipAddress(ipAddress)
                .status(success ? 1 : 0)
                .operationDesc(remark)
                .createTime(LocalDateTime.now())
                .build();
        createLog(log);
    }

    @Async
    @Override
    public void recordLoginFailure(String username, String ipAddress, String reason) {
        SysAuditLog log = SysAuditLog.builder()
                .username(username)
                .operationType("LOGIN_FAILURE")
                .ipAddress(ipAddress)
                .status(0)
                .errorMsg(reason)
                .createTime(LocalDateTime.now())
                .build();
        createLog(log);
    }

    @Async
    @Override
    public void recordLogout(UUID userId, String remark) {
        SysAuditLog log = SysAuditLog.builder()
                .userId(userId != null ? userId.toString() : null)
                .operationType("LOGOUT")
                .status(1)
                .operationDesc(remark)
                .createTime(LocalDateTime.now())
                .build();
        createLog(log);
    }

    @Async
    @Override
    public void recordSecurityEvent(String eventType, Integer riskLevel, UUID userId, String username, String ipAddress,
                                    String resource, boolean success, String details) {
        try {
            SysAuditLog auditLog = SysAuditLog.builder()
                    .userId(userId != null ? userId.toString() : null)
                    .username(username)
                    .operationType(eventType)
                    .riskLevel(riskLevel)
                    .ipAddress(ipAddress)
                    .operationModule(resource)
                    .operationDesc(details)
                    .status(success ? 1 : 0)
                    .createTime(LocalDateTime.now())
                    .build();
            createLog(auditLog);

            if (riskLevel != null && riskLevel >= 4) {
                log.warn("Security Alert: {}, User: {}, IP: {}",
                        (auditLog.getOperationType() + "").replace("\r", "_").replace("\n", "_"), (auditLog.getUsername() + "").replace("\r", "_").replace("\n", "_"), (auditLog.getIpAddress() + "").replace("\r", "_").replace("\n", "_"));
            }
        } catch (Exception e) {
            log.error("Failed to record security event", e);
        }
    }

    @Override
    public void record(com.cognora.platform.common.log.dto.AuditLogRecord record) {
        SysAuditLog entity = SysAuditLog.builder()
                .userId(record.getUserId())
                .username(record.getUsername())
                .realName(record.getRealName())
                .deptId(record.getDeptId())
                .operationType(record.getOperationType())
                .operationModule(record.getOperationModule())
                .operationDesc(record.getOperationDesc())
                .requestUri(record.getRequestUri())
                .requestMethod(record.getRequestMethod())
                .requestParams(record.getRequestParams())
                .responseData(record.getResponseData())
                .responseStatus(record.getResponseStatus())
                .ipAddress(record.getIpAddress())
                .location(record.getLocation())
                .userAgent(record.getUserAgent())
                .businessType(record.getBusinessType())
                .businessId(record.getBusinessId())
                .oldValue(record.getOldValue())
                .newValue(record.getNewValue())
                .riskLevel(record.getRiskLevel())
                .status(record.getStatus())
                .errorMsg(record.getErrorMsg())
                .executeTime(record.getExecuteTime())
                .createTime(record.getCreateTime())
                .build();
        createLog(entity);
        recordSensitiveOperation(entity);
    }

    /**
     * High-risk {@code @AuditLog} operations are mirrored into the sensitive-operation ledger, so
     * that table is fed by the audit chain instead of only by hand-written API calls. A failure
     * here must not lose the general audit row, which is already durable.
     */
    private void recordSensitiveOperation(SysAuditLog auditLog) {
        Integer riskLevel = auditLog.getRiskLevel();
        if (riskLevel == null || riskLevel < SENSITIVE_RISK_THRESHOLD) {
            return;
        }
        if (!StringUtils.hasText(auditLog.getUserId())) {
            log.warn("Sensitive operation has no actor and is not mirrored: type={}",
                    auditLog.getOperationType());
            return;
        }
        try {
            SysSensitiveOperationLog sensitive = new SysSensitiveOperationLog()
                    .setUserId(auditLog.getUserId())
                    .setUsername(auditLog.getUsername())
                    .setOperationType(auditLog.getOperationType())
                    .setOperationModule(auditLog.getOperationModule())
                    .setSensitiveDataType(StringUtils.hasText(auditLog.getBusinessType())
                            ? auditLog.getBusinessType()
                            : "AUDITED_OPERATION")
                    .setAffectedCount(1)
                    .setTargetTable(auditLog.getBusinessType())
                    .setRiskScore(Math.min(riskLevel, 10))
                    .setApprovalRequired(Boolean.FALSE)
                    .setIpAddress(auditLog.getIpAddress())
                    .setUserAgent(auditLog.getUserAgent())
                    .setCreateTime(auditLog.getCreateTime());
            sensitiveOperationLogCommandService.createLog(sensitive);
        } catch (Exception e) {
            log.error("Failed to mirror sensitive operation: type={}", auditLog.getOperationType(), e);
        }
    }
}
