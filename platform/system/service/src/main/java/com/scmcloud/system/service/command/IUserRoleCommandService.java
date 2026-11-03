package com.scmcloud.system.service.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Command-side service for the user-role association lifecycle, including
 * temporary role grants.
 *
 * <p>Backed by {@link UserRoleCrossDatabaseCommandService}; this is the user-facing
 * facade for {@code db_permission} mutations that don't load a {@code SysUser}
 * entity directly.</p>
 */
public interface IUserRoleCommandService {

    void grantTemporaryRoles(UUID userId, List<UUID> roleIds,
                             LocalDateTime effectiveTime, LocalDateTime expireTime);

    void extendTemporaryRole(UUID userId, UUID roleId, LocalDateTime newExpireTime);

    void terminateTemporaryRole(UUID userId, UUID roleId);
}