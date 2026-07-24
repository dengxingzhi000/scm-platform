package com.scmcloud.system.task;

import com.scmcloud.system.mapper.SysUserRoleMapper;
import com.scmcloud.system.notification.NotificationService;
import com.scmcloud.system.notification.model.NotificationChannel;
import com.scmcloud.system.notification.model.NotificationCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 权限过期检查定时任务
 *
 * @author Deng
 * @since 2025-10-30
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionExpiryTask {
    private final SysUserRoleMapper userRoleMapper;
    private final NotificationService notificationService;

    @Value("${webauthn.expiry-notify-days:7}")
    private int expiryNotifyDays;

    /**
     * 每天凌晨2点检查并处理过期权限
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkExpiredPermissions() {
        log.info("Starting expired permissions check task");

        try {
            List<ExpiredRoleInfo> expiredRoles = findExpiredRoles();

            if (!expiredRoles.isEmpty()) {
                log.warn("Found {} expired roles", expiredRoles.size());

                for (ExpiredRoleInfo role : expiredRoles) {
                    log.info("Expired role: user={}, role={}, expireTime={}",
                            role.username(), role.roleName(), role.expireTime());
                }

                int updatedCount = userRoleMapper.updateExpiredRolesStatus();
                log.info("Updated {} expired role assignments", updatedCount);

                sendNotifications(expiredRoles, "permission-expired",
                        "Permission expired",
                        "permission.expired",
                        (username, roleName, expireTime) ->
                                String.format("Hello %s, your assigned role %s has expired.", username, roleName));
            }

            log.info("Expired permissions check completed");
        } catch (Exception e) {
            log.error("Error during expired permissions check", e);
        }
    }

    /**
     * 每天上午9点检查即将过期的权限（提前N天通知）
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkExpiringPermissions() {
        log.info("Starting expiring permissions check task");

        try {
            List<ExpiredRoleInfo> expiringRoles = findExpiringRoles(expiryNotifyDays);

            if (!expiringRoles.isEmpty()) {
                log.info("Found {} roles expiring in {} days", expiringRoles.size(), expiryNotifyDays);

                sendNotifications(expiringRoles, "permission-expiring",
                        "Permission expiring soon",
                        "permission.expiring",
                        (username, roleName, expireTime) ->
                                String.format("Hello %s, your role %s will expire on %s. Please renew if needed.",
                                        username, roleName, expireTime));
            }

            log.info("Expiring permissions check completed");
        } catch (Exception e) {
            log.error("Error during expiring permissions check", e);
        }
    }

    /**
     * 每周一凌晨3点清理过期权限数据（可选）
     * 如果不需要保留过期数据用于审计，可以启用此任务
     */
    @Scheduled(cron = "0 0 3 ? * MON")
    public void cleanupExpiredPermissions() {
        log.info("Starting cleanup of expired permissions");

        try {
            int deletedCount = userRoleMapper.deleteExpiredRoles();
            log.info("Cleaned up {} expired role assignments", deletedCount);
        } catch (Exception e) {
            log.error("Error during expired permissions cleanup", e);
        }
    }

    private List<ExpiredRoleInfo> findExpiredRoles() {
        List<Map<String, Object>> rows = userRoleMapper.findExpiredRolesForCleanup();
        List<ExpiredRoleInfo> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            result.add(new ExpiredRoleInfo(
                    (String) row.get("username"),
                    null,
                    (String) row.get("role_name"),
                    toLocalDateTime(row.get("expire_time"))
            ));
        }
        return result;
    }

    private List<ExpiredRoleInfo> findExpiringRoles(int days) {
        List<Map<String, Object>> rows = userRoleMapper.findExpiringRolesForNotification(days);
        List<ExpiredRoleInfo> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            result.add(new ExpiredRoleInfo(
                    (String) row.get("username"),
                    null,
                    (String) row.get("role_name"),
                    toLocalDateTime(row.get("expire_time"))
            ));
        }
        return result;
    }

    private void sendNotifications(List<ExpiredRoleInfo> roles, String referencePrefix,
                                   String subject, String templateCode,
                                   ContentFormatter formatter) {
        for (ExpiredRoleInfo role : roles) {
            try {
                String content = formatter.format(role.username(), role.roleName(), role.expireTime());
                String referenceId = referencePrefix + "-" + role.roleName() + "-" + role.username();

                NotificationCommand command = NotificationCommand.builder()
                        .referenceId(referenceId)
                        .username(role.username())
                        .email(role.email())
                        .subject(subject)
                        .content(content)
                        .templateCode(templateCode)
                        .channel(NotificationChannel.EMAIL)
                        .channel(NotificationChannel.SYSTEM_MESSAGE)
                        .variable("username", role.username())
                        .variable("roleName", role.roleName())
                        .build();
                notificationService.send(command);
            } catch (Exception e) {
                log.error("Failed to send notification for role: {}", role.roleName(), e);
            }
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        return null;
    }

    @FunctionalInterface
    private interface ContentFormatter {
        String format(String username, String roleName, LocalDateTime expireTime);
    }
}
