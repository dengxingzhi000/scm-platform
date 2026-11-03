package com.scmcloud.common.log.service;

import com.scmcloud.common.log.dto.AuditLogRecord;
import com.scmcloud.common.rest.client.SysAuditServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Delivers audit records to scm-audit over HTTP instead of writing them locally.
 *
 * <p>scm-audit owns {@code db_audit}; every other service now reports through this sink, so
 * {@code @AuditLog} works wherever the shared library is on the classpath (previously the only
 * aspect lived inside scm-audit and was never scanned by the business services).
 *
 * <p>Delivery is best-effort: a failed audit call is logged and swallowed so it can never break
 * the business operation. Calls made before a token exists (e.g. login failures) have no
 * authenticated context and will fall back to the log line.
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteSysAuditLogService implements ISysAuditLogService {

    private final SysAuditServiceClient auditServiceClient;

    @Override
    public void record(AuditLogRecord record) {
        if (record.getCreateTime() == null) {
            record.setCreateTime(LocalDateTime.now());
        }
        try {
            auditServiceClient.create(record);
        } catch (Exception e) {
            log.warn("Audit record delivery failed: type={}, desc={}, user={}: {}",
                    sanitize(record.getOperationType()), sanitize(record.getOperationDesc()),
                    sanitize(record.getUsername()), e.getMessage());
        }
    }

    @Override
    public void recordLogin(UUID userId, String username, String ipAddress, boolean success, String remark) {
        record(AuditLogRecord.builder()
                .userId(userId != null ? userId.toString() : null)
                .username(username)
                .ipAddress(ipAddress)
                .operationType(success ? "LOGIN" : "LOGIN_FAILURE")
                .operationDesc(success ? "用户登录" : "用户登录失败")
                .status(success ? 1 : 0)
                .errorMsg(success ? null : remark)
                .riskLevel(1)
                .build());
    }

    @Override
    public void recordLoginFailure(String username, String ipAddress, String reason) {
        record(AuditLogRecord.builder()
                .username(username)
                .ipAddress(ipAddress)
                .operationType("LOGIN_FAILURE")
                .operationDesc("用户登录失败")
                .errorMsg(reason)
                .status(0)
                .riskLevel(2)
                .build());
    }

    @Override
    public void recordLogout(UUID userId, String remark) {
        record(AuditLogRecord.builder()
                .userId(userId != null ? userId.toString() : null)
                .operationType("LOGOUT")
                .operationDesc("用户登出")
                .status(1)
                .riskLevel(1)
                .build());
    }

    @Override
    public void recordSecurityEvent(String eventType, Integer riskLevel, UUID userId, String username,
                                    String ipAddress, String resource, boolean success, String details) {
        record(AuditLogRecord.builder()
                .userId(userId != null ? userId.toString() : null)
                .username(username)
                .ipAddress(ipAddress)
                .operationType(eventType)
                .operationModule("SECURITY")
                .operationDesc(details)
                .requestUri(resource)
                .riskLevel(riskLevel)
                .status(success ? 1 : 0)
                .errorMsg(success ? null : details)
                .build());
    }

    private static String sanitize(String value) {
        return (value + "").replace("\r", "_").replace("\n", "_");
    }
}
