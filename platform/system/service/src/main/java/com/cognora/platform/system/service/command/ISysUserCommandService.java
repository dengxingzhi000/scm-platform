package com.cognora.platform.system.service.command;

import com.cognora.platform.common.dto.user.UpdateProfileRequest;
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

    /**
     * 当前登录用户修改自己的资料。
     *
     * <p>与 {@link #updateUser(UUID, UserDTO)} 完全独立：不接受目标 userId，只更新
     * 昵称/头像/手机/邮箱/时区五项白名单字段，不触碰角色、状态、部门、密码。</p>
     *
     * @return 与 {@code GET /api/system/users/{id}} 同结构的用户信息
     */
    UserDTO updateOwnProfile(UpdateProfileRequest request);

    void deleteUser(UUID id);

    String resetPassword(UUID id);

    void changePassword(String oldPassword, String newPassword);

    void grantRoles(UUID userId, List<UUID> roleIds);

    void lockUser(UUID id, Boolean lock);

    void forceLogout(UUID id, String reason);

    void updateLastLogin(UUID userId, String ipAddress);

    void updateLastLogin(UUID userId, String ipAddress, LocalDateTime loginTime);
}
