package com.cognora.platform.system.service.command;

import com.cognora.platform.common.dto.user.UserDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Command-side service for {@link com.cognora.platform.system.domain.entity.SysUser}.
 *
 * <p>All methods are mutating. Implementations must run inside a write transaction
 * and must evict tenant-scoped cache entries after mutation.</p>
 */
public interface ISysUserCommandService {

    void addUser(UserDTO userDTO);

    void updateUser(UUID id, UserDTO userDTO);

    void deleteUser(UUID id);

    String resetPassword(UUID id);

    void changePassword(String oldPassword, String newPassword);

    void grantRoles(UUID userId, List<UUID> roleIds);

    void lockUser(UUID id, Boolean lock);

    void forceLogout(UUID id, String reason);

    void updateLastLogin(UUID userId, String ipAddress);

    void updateLastLogin(UUID userId, String ipAddress, LocalDateTime loginTime);
}
