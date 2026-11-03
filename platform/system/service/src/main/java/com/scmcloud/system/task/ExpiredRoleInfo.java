package com.scmcloud.system.task;

import java.time.LocalDateTime;

/**
 * 过期角色信息
 *
 * @author Deng
 * @since 2025-10-30
 */
public record ExpiredRoleInfo(
        String username,
        String email,
        String roleName,
        LocalDateTime expireTime
) {
}
