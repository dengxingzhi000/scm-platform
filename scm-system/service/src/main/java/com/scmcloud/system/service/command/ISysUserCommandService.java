package com.scmcloud.system.service.command;

import com.scmcloud.common.dto.user.UserDTO;

import java.util.List;
import java.util.UUID;

/**
 * Command-side service for {@link com.scmcloud.system.domain.entity.SysUser}.
 *
 * <p>All methods are mutating. Implementations must run inside a write transaction
 * and must evict tenant-scoped cache entries after mutation.</p>
 */
public interface ISysUserCommandService {

    void addUser(UserDTO userDTO);

    void updateUser(UserDTO userDTO);

    void deleteUser(UUID id);

    String resetPassword(UUID id);

    void changePassword(UUID userId, String oldPassword, String newPassword);

    void grantRoles(UUID userId, List<UUID> roleIds);

    void lockUser(UUID id, Boolean lock);

    void updateLastLogin(UUID userId, String ipAddress);
}
