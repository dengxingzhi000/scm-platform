package com.scmcloud.common.log.service;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Fallback audit sink: the only other implementation lives inside scm-audit's own service module,
 * so services that audit logins (scm-auth) could not start. This records the events through the
 * application log; a persistent implementation can replace it by simply declaring another
 * {@link ISysAuditLogService} bean.
 */
@Slf4j
public class LoggingSysAuditLogService implements ISysAuditLogService {

    @Override
    public void recordLogin(UUID userId, String username, String ipAddress, boolean success, String remark) {
        log.info("audit login user={} id={} ip={} success={} remark={}", (username + "").replace("\r", "_").replace("\n", "_"), (userId + "").replace("\r", "_").replace("\n", "_"), (ipAddress + "").replace("\r", "_").replace("\n", "_"), (success + "").replace("\r", "_").replace("\n", "_"), (remark + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    public void recordLoginFailure(String username, String ipAddress, String reason) {
        log.warn("audit login failure user={} ip={} reason={}", (username + "").replace("\r", "_").replace("\n", "_"), (ipAddress + "").replace("\r", "_").replace("\n", "_"), (reason + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    public void recordLogout(UUID userId, String remark) {
        log.info("audit logout id={} remark={}", (userId + "").replace("\r", "_").replace("\n", "_"), (remark + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    public void recordSecurityEvent(String eventType, Integer riskLevel, UUID userId, String username,
                                    String ipAddress, String resource, boolean success, String details) {
        log.info("audit security event={} risk={} user={} id={} ip={} resource={} success={} details={}",
                (eventType + "").replace("\r", "_").replace("\n", "_"), (riskLevel + "").replace("\r", "_").replace("\n", "_"), (username + "").replace("\r", "_").replace("\n", "_"), (userId + "").replace("\r", "_").replace("\n", "_"), (ipAddress + "").replace("\r", "_").replace("\n", "_"), (resource + "").replace("\r", "_").replace("\n", "_"), (success + "").replace("\r", "_").replace("\n", "_"), (details + "").replace("\r", "_").replace("\n", "_"));
    }
}
