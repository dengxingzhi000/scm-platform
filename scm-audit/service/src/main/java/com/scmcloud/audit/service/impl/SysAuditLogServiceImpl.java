package com.scmcloud.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.common.log.service.ISysAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.scmcloud.audit.domain.entity.SysAuditLog;
import com.scmcloud.audit.mapper.SysAuditLogMapper;
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
        implements ISysAuditLogService, com.scmcloud.audit.service.ISysAuditLogService {

    public SysAuditLog createLog(SysAuditLog entity) {
        log.info("创建审计日志: userId={}, operationType={}, module={}",
                entity.getUserId(), entity.getOperationType(), entity.getOperationModule());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        save(entity);
        log.info("审计日志创建成功: id={}", entity.getId());
        return entity;
    }

    public SysAuditLog getById(String id) {
        return lambdaQuery()
                .eq(SysAuditLog::getId, id)
                .one();
    }

    public boolean deleteById(String id) {
        log.info("删除审计日志: id={}", id);
        return removeById(id);
    }

    public List<SysAuditLog> listByUserId(String userId) {
        return lambdaQuery()
                .eq(SysAuditLog::getUserId, userId)
                .orderByDesc(SysAuditLog::getCreateTime)
                .list();
    }

    public List<SysAuditLog> listByModule(String module) {
        return lambdaQuery()
                .eq(SysAuditLog::getOperationModule, module)
                .orderByDesc(SysAuditLog::getCreateTime)
                .list();
    }

    public List<SysAuditLog> listByBusinessType(String businessType) {
        return lambdaQuery()
                .eq(SysAuditLog::getBusinessType, businessType)
                .orderByDesc(SysAuditLog::getCreateTime)
                .list();
    }

    public Page<SysAuditLog> pageQuery(int page, int size, String userId, String operationType,
                                        String module, String businessType, Integer riskLevel) {
        log.debug("分页查询审计日志: page={}, size={}, userId={}, operationType={}, module={}, businessType={}, riskLevel={}",
                page, size, userId, operationType, module, businessType, riskLevel);

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
        save(log);
    }

    @Async
    public void recordLoginFailure(String username, String ipAddress, String reason) {
        SysAuditLog log = SysAuditLog.builder()
                .username(username)
                .operationType("LOGIN_FAILURE")
                .ipAddress(ipAddress)
                .status(0)
                .errorMsg(reason)
                .createTime(LocalDateTime.now())
                .build();
        save(log);
    }

    @Async
    public void recordLogout(UUID userId, String remark) {
        SysAuditLog log = SysAuditLog.builder()
                .userId(userId != null ? userId.toString() : null)
                .operationType("LOGOUT")
                .status(1)
                .operationDesc(remark)
                .createTime(LocalDateTime.now())
                .build();
        save(log);
    }

    @Async
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
            save(auditLog);

            if (riskLevel != null && riskLevel >= 4) {
                log.warn("Security Alert: {}, User: {}, IP: {}",
                        auditLog.getOperationType(), auditLog.getUsername(), auditLog.getIpAddress());
            }
        } catch (Exception e) {
            log.error("Failed to record security event", e);
        }
    }
}
