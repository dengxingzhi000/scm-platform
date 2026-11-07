package com.cognora.platform.system.service.command;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cognora.platform.common.dto.user.CreateUserRequest;
import com.cognora.platform.common.dto.user.ResetPasswordResponse;
import com.cognora.platform.common.dto.user.UpdateProfileRequest;
import com.cognora.platform.common.dto.user.UpdateUserRequest;
import com.cognora.platform.common.dto.user.UserDTO;
import com.cognora.platform.common.dto.user.UserSessionDTO;
import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.rest.client.SysAuthServiceClient;
import com.cognora.platform.common.security.PermissionChecker;
import com.cognora.platform.common.status.StatusValidator;
import com.cognora.platform.common.tenant.TenantValidationUtil;
import com.cognora.platform.common.util.UUIDv7Util;
import com.cognora.platform.common.web.util.SecurityUtils;
import com.cognora.platform.system.cache.UserAuthorizationCacheEvictor;
import com.cognora.platform.system.domain.entity.SysUser;
import com.cognora.platform.system.domain.enums.UserStatus;
import com.cognora.platform.system.event.DataSyncEventPublisher;
import com.cognora.platform.system.mapper.SysUserMapper;
import com.cognora.platform.system.service.query.ISysUserQueryService;
import com.cognora.platform.system.service.query.RoleCrossDatabaseQueryService;
import com.cognora.platform.system.service.query.UserCrossDatabaseQueryService;
import com.cognora.platform.system.service.support.PasswordPolicyService;
import com.cognora.platform.system.service.support.UserReadAccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Write-side service for {@link SysUser}. Uses {@link UserCommandTemplate} to
 * encapsulate the standard 8-step boilerplate for commands that mutate an existing
 * user with data-scope enforcement (updateUser, deleteUser). {@code grantRoles}
 * intentionally bypasses the data-scope check (matching the original behaviour),
 * and {@code addUser}, {@code resetPassword}, {@code changePassword}, {@code lockUser},
 * {@code updateLastLogin} follow simpler flows and inline their checks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserCommandServiceImpl implements ISysUserCommandService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";

    private final SysUserMapper userMapper;
    private final ISysUserQueryService sysUserQueryService;
    private final UserCrossDatabaseQueryService userQueryService;
    private final RoleCrossDatabaseQueryService roleQueryService;
    private final UserRoleCrossDatabaseCommandService userRoleCommandService;
    private final PasswordEncoder passwordEncoder;
    private final DataSyncEventPublisher dataSyncEventPublisher;
    private final PermissionChecker permissionChecker;
    private final StatusValidator statusValidator;
    private final UserCommandTemplate commandTemplate;
    private final SysAuthServiceClient authServiceClient;
    private final UserAuthorizationCacheEvictor cacheEvictor;
    private final PasswordPolicyService passwordPolicyService;
    private final UserReadAccessGuard userReadAccessGuard;

    @Value("${app.security.admin-user-id:019a0aee-3b74-7bfc-b34f-48b5428d4875}")
    private String adminUserId;

    @Value("${spring.security.default-password}")
    private String defaultPassword;

    @Override
    // 跨 db_user（用户）与 db_permission（用户角色）：必须用 @DSTransactional，
    // 普通 @Transactional 会锁定主库连接，导致 @DS("permission") 路由失效。
    @DSTransactional
    public void addUser(CreateUserRequest request) {
        UUID tenantId = TenantValidationUtil.getRequiredTenantId();
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "user:add");
        validateRoleAssignment(operatorId, request.getRoleIds());

        if (userMapper.existsByUsername(tenantId, request.getUsername())) {
            throw new BusinessException(ResultCode.USER_EXIST.getCode(),
                    ResultCode.USER_EXIST.getMessage());
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty()
                && userMapper.existsByPhone(tenantId, request.getPhone(), null)) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "手机号已被使用");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()
                && userMapper.existsByEmail(tenantId, request.getEmail(), null)) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "邮箱已被使用");
        }

        boolean explicitPassword = request.getPassword() != null && !request.getPassword().isEmpty();
        if (explicitPassword) {
            passwordPolicyService.validate(request.getPassword());
        }
        String rawPassword = explicitPassword ? request.getPassword() : defaultPassword;
        String encoded = passwordEncoder.encode(rawPassword);

        SysUser user = new SysUser();
        user.setId(UUIDv7Util.generate());
        user.setTenantId(tenantId);
        user.setUsername(request.getUsername());
        user.setPassword(encoded);
        user.setRealName(request.getRealName());
        user.setIdCard(emptyToNull(request.getIdCard()));
        user.setEmail(emptyToNull(request.getEmail()));
        user.setPhone(emptyToNull(request.getPhone()));
        user.setAvatar(request.getAvatar());
        user.setTimezone(request.getTimezone());
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setDeptId(request.getDeptId());
        user.setUserLevel(request.getUserLevel());
        user.setAccountType(request.getAccountType());
        user.setRemark(request.getRemark());
        user.setPasswordExpireTime(LocalDateTime.now().plusDays(passwordPolicyService.getExpireDays()));
        user.setForceChangePassword(true);
        user.setLoginAttempts(0);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "用户名、手机号或邮箱已存在");
        }

        passwordPolicyService.record(user.getId(), encoded, "INITIAL");

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            int inserted = userRoleCommandService.batchInsertUserRoles(
                    user.getId(), request.getRoleIds(), operatorId);
            log.debug("创建用户时分配角色: user={}, roleCount={}", user.getUsername(), inserted);
        }

        // 新用户没有既有精确键，但 username 可能存在 null 的 userDetails 缓存，按精确键清理。
        cacheEvictor.evictUserProfile(user.getId(), user.getUsername());

        dataSyncEventPublisher.publishUserCreated(user);
        TenantValidationUtil.logTenantOperation("CREATE", "USER", user.getId());

        log.info("用户创建成功: username={}, operator={}", user.getUsername(),
                SecurityUtils.getCurrentUsername());
    }

    @Override
    @DSTransactional
    public void updateUser(UUID id, UpdateUserRequest request) {
        UUID tenantId = TenantValidationUtil.getRequiredTenantId();
        SysUser user = commandTemplate.execute("UPDATE", "user:update", id,
                (loaded, op) -> {
                    int currentVersion = loaded.getVersion() != null ? loaded.getVersion() : 0;
                    if (request.getVersion() != null && request.getVersion() != currentVersion) {
                        throw new BusinessException(ResultCode.CONFLICT.getCode(),
                                "用户数据已被其他操作修改，请刷新后重试");
                    }
                    String phone = emptyToNull(request.getPhone());
                    if (phone != null && !phone.equals(loaded.getPhone())
                            && userMapper.existsByPhone(tenantId, phone, loaded.getId())) {
                        throw new BusinessException(ResultCode.CONFLICT.getCode(), "手机号已被使用");
                    }
                    String email = emptyToNull(request.getEmail());
                    if (email != null && !email.equals(loaded.getEmail())
                            && userMapper.existsByEmail(tenantId, email, loaded.getId())) {
                        throw new BusinessException(ResultCode.CONFLICT.getCode(), "邮箱已被使用");
                    }

                    SysUser update = new SysUser();
                    update.setId(loaded.getId());
                    update.setVersion(loaded.getVersion());
                    boolean changed = false;
                    if (request.getRealName() != null) {
                        if (request.getRealName().isBlank()) {
                            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(),
                                    "真实姓名不能为空");
                        }
                        update.setRealName(request.getRealName());
                        changed = true;
                    }
                    if (request.getIdCard() != null && !request.getIdCard().isEmpty()) {
                        update.setIdCard(request.getIdCard());
                        changed = true;
                    }
                    if (email != null) {
                        update.setEmail(email);
                        changed = true;
                    }
                    if (phone != null) {
                        update.setPhone(phone);
                        changed = true;
                    }
                    if (request.getAvatar() != null) {
                        update.setAvatar(request.getAvatar());
                        changed = true;
                    }
                    if (request.getTimezone() != null) {
                        update.setTimezone(request.getTimezone());
                        changed = true;
                    }
                    if (request.getDeptId() != null) {
                        update.setDeptId(request.getDeptId());
                        changed = true;
                    }
                    if (request.getUserLevel() != null) {
                        update.setUserLevel(request.getUserLevel());
                        changed = true;
                    }
                    if (request.getAccountType() != null) {
                        update.setAccountType(request.getAccountType());
                        changed = true;
                    }
                    if (request.getRemark() != null) {
                        update.setRemark(request.getRemark());
                        changed = true;
                    }

                    if (!changed) {
                        return;
                    }
                    int updatedRows;
                    try {
                        updatedRows = userMapper.updateById(update);
                    } catch (DuplicateKeyException ex) {
                        throw new BusinessException(ResultCode.CONFLICT.getCode(), "手机号或邮箱已存在");
                    }
                    if (updatedRows == 0) {
                        throw new BusinessException(ResultCode.CONFLICT.getCode(),
                                "用户数据已被其他操作修改，请刷新后重试");
                    }
                });

        cacheEvictor.evictUserAuthorization(user.getId(), user.getUsername());
        if (request.getDeptId() != null && !request.getDeptId().equals(user.getDeptId())) {
            cacheEvictor.evictUserDeptId(user.getId());
        }

        SysUser updated = userMapper.selectById(user.getId());
        dataSyncEventPublisher.publishUserUpdated(updated);

        log.info("用户更新成功: username={}, operator={}", user.getUsername(),
                SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO updateOwnProfile(UpdateProfileRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid()
                .orElseThrow(() -> new BusinessException(
                        ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage()));

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }
        TenantValidationUtil.validateDataOwnership(user.getTenantId());

        // 只接受 DTO 里的五项白名单；request 中不存在的字段（用户名、密码、状态、部门、
        // 租户等）根本没有 setter 入口，因此不会被批量赋值。
        boolean changed = false;
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
            changed = true;
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
            changed = true;
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
            changed = true;
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
            changed = true;
        }
        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
            changed = true;
        }

        if (changed) {
            userMapper.updateById(user);
            cacheEvictor.evictUserProfile(user.getId(), user.getUsername());
            dataSyncEventPublisher.publishUserUpdated(user);
            log.info("个人资料更新成功: userId={}", userId);
        }

        // 与 GET /api/system/users/{id} 返回同一个 UserDTO 结构（含部门与角色）。
        return sysUserQueryService.getUserById(userId);
    }

    @Override
    @DSTransactional
    public void deleteUser(UUID id) {
        SysUser user = commandTemplate.execute("DELETE", "user:delete", id,
                (loaded, op) -> {
                    UUID adminId = UUID.fromString(adminUserId);
                    if (loaded.getId().equals(adminId)) {
                        throw new BusinessException(
                                ResultCode.USER_CANNOT_DELETE_ADMIN.getCode(),
                                ResultCode.USER_CANNOT_DELETE_ADMIN.getMessage());
                    }
                    if (loaded.getId().equals(op)) {
                        throw new BusinessException(
                                ResultCode.USER_CANNOT_DELETE_SELF.getCode(),
                                ResultCode.USER_CANNOT_DELETE_SELF.getMessage());
                    }
                    // 同时清理 db_permission 中的用户角色，避免留下孤儿授权。
                    userRoleCommandService.deleteUserRoles(loaded.getId());
                    userMapper.deleteById(loaded.getId());
                    dataSyncEventPublisher.publishUserDeleted(loaded.getId());
                });

        cacheEvictor.evictUserAuthorization(user.getId(), user.getUsername());
        cacheEvictor.evictUserDept(user.getId());

        log.info("用户删除成功: username={}, operator={}", user.getUsername(),
                SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResetPasswordResponse resetPassword(UUID id) {
        SysUser user = userReadAccessGuard.requireReadable(id);

        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        boolean reveal = passwordPolicyService.isRevealResetPassword()
                && permissionChecker.hasPermission(operatorId, "system:user:reset:reveal");

        String newPassword = generateRandomPassword();
        passwordPolicyService.validate(newPassword);
        String encoded = passwordEncoder.encode(newPassword);
        LocalDateTime expireAt = LocalDateTime.now()
                .plusDays(passwordPolicyService.getExpireDays());

        user.setPassword(encoded);
        user.setForceChangePassword(true);
        user.setLastPasswordChangeTime(LocalDateTime.now());
        user.setPasswordExpireTime(expireAt);
        int updatedRows = userMapper.updateById(user);
        if (updatedRows == 0) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(),
                    "用户数据已被其他操作修改，请刷新后重试");
        }

        passwordPolicyService.record(user.getId(), encoded, "RESET");
        cacheEvictor.evictUserCredentials(user.getId(), user.getUsername());

        // 重置密码后强制下线，避免旧会话继续持有已失效的凭据。
        authServiceClient.forceLogout(id, "Password reset by administrator");

        log.info("Password reset for user: {}, by: {}",
                user.getUsername(), SecurityUtils.getCurrentUsername());
        return ResetPasswordResponse.builder()
                .temporaryPassword(reveal ? newPassword : null)
                .mustChangePassword(true)
                .expiresAt(expireAt)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String oldPassword, String newPassword) {
        doChangePassword(SecurityUtils.getCurrentUserUuid().orElse(null), oldPassword, newPassword);
    }

    private void doChangePassword(UUID userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(
                    ResultCode.USER_PASSWORD_INCORRECT_OLD.getCode(),
                    ResultCode.USER_PASSWORD_INCORRECT_OLD.getMessage());
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(
                    ResultCode.USER_PASSWORD_SAME_AS_OLD.getCode(),
                    ResultCode.USER_PASSWORD_SAME_AS_OLD.getMessage());
        }
        passwordPolicyService.validate(newPassword);
        passwordPolicyService.validateNotReused(userId, newPassword);

        LocalDateTime expireAt = LocalDateTime.now()
                .plusDays(passwordPolicyService.getExpireDays());
        String encoded = passwordEncoder.encode(newPassword);
        user.setPassword(encoded);
        user.setForceChangePassword(false);
        user.setLastPasswordChangeTime(LocalDateTime.now());
        user.setPasswordExpireTime(expireAt);
        int updatedRows = userMapper.updateById(user);
        if (updatedRows == 0) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(),
                    "用户数据已被其他操作修改，请重试");
        }

        passwordPolicyService.record(user.getId(), encoded, "CHANGE");
        cacheEvictor.evictUserCredentials(user.getId(), user.getUsername());

        log.info("Password changed for user: {}", user.getUsername());
    }

    @Override
    @DSTransactional
    public void grantRoles(UUID userId, List<UUID> roleIds) {
        // Preserves the original behaviour: data-scope check is NOT applied for role grants,
        // only tenant ownership and role-level checks. Don't use UserCommandTemplate here.
        TenantValidationUtil.getRequiredTenantId();
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "user:grant-role");

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }
        TenantValidationUtil.validateDataOwnership(user.getTenantId());

        validateRoleAssignment(operatorId, roleIds);

        int deleted = userRoleCommandService.deleteUserRoles(user.getId());
        log.debug("授权操作清除原有角色: user={}, deletedCount={}",
                user.getUsername(), deleted);
        if (roleIds != null && !roleIds.isEmpty()) {
            int inserted = userRoleCommandService.batchInsertUserRoles(
                    user.getId(), roleIds, operatorId);
            log.debug("授权操作分配新角色: user={}, grantedCount={}",
                    user.getUsername(), inserted);
        }

        cacheEvictor.evictUserAuthorization(user.getId(), user.getUsername());

        TenantValidationUtil.logTenantOperation("GRANT_ROLES", "USER", userId);

        log.info("角色授予操作完成: user={}, roleIds={}, operator={}",
                (user.getUsername() + "").replace("\r", "_").replace("\n", "_"), (roleIds + "").replace("\r", "_").replace("\n", "_"), (SecurityUtils.getCurrentUsername() + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(UUID id) {
        changeLifecycleStatus(id, UserStatus.ACTIVE, null, null, true, "ENABLE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(UUID id, String reason) {
        changeLifecycleStatus(id, UserStatus.INACTIVE, reason, null, false, "DISABLE");
        authServiceClient.forceLogout(id, reason != null && !reason.isBlank()
                ? reason : "Account disabled by administrator");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockUser(UUID id, String reason, Integer durationMinutes) {
        int minutes = durationMinutes != null && durationMinutes > 0 ? durationMinutes : 1440;
        changeLifecycleStatus(id, UserStatus.LOCKED, reason,
                LocalDateTime.now().plusMinutes(minutes), false, "LOCK");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockUser(UUID id) {
        changeLifecycleStatus(id, UserStatus.ACTIVE, null, null, true, "UNLOCK");
    }

    @Override
    public List<UserSessionDTO> listUserSessions(UUID id) {
        userReadAccessGuard.requireReadable(id);
        ApiResponse<List<UserSessionDTO>> response = authServiceClient.listUserSessions(id);
        if (response == null || response.code() != 200 || response.data() == null) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE.getCode(),
                    "认证服务不可用，无法查询会话");
        }
        return response.data();
    }

    @Override
    public int revokeUserSession(UUID id, String sessionId, String reason) {
        userReadAccessGuard.requireReadable(id);
        ApiResponse<Integer> response = authServiceClient.revokeUserSession(id, sessionId,
                reason != null && !reason.isBlank() ? reason : "Session revoked by administrator");
        if (response == null || response.code() != 200 || response.data() == null) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE.getCode(),
                    "认证服务不可用，无法撤销会话");
        }
        return response.data();
    }

    /**
     * 账号生命周期状态变更。使用 UpdateWrapper 显式清空锁定字段，
     * 避免 MyBatis-Plus 默认 NOT_NULL 策略把 null 字段跳过。
     */
    private void changeLifecycleStatus(UUID id, UserStatus target, String reason,
                                       LocalDateTime lockedUntil, boolean clearAttempts,
                                       String operation) {
        SysUser user = commandTemplate.execute(operation, "user:update", id, (loaded, op) -> {
            UserStatus from = UserStatus.fromCode(loaded.getStatus());
            statusValidator.validateTransition("USER", from.name(), target.name());

            LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<>();
            update.eq(SysUser::getId, loaded.getId())
                    .set(SysUser::getStatus, target.getCode())
                    .set(SysUser::getLockReason, reason)
                    .set(SysUser::getLockedUntil, lockedUntil)
                    .set(SysUser::getStatusChangedAt, LocalDateTime.now())
                    .set(SysUser::getStatusChangedBy, op)
                    .setSql("version = version + 1");
            if (clearAttempts) {
                update.set(SysUser::getLoginAttempts, 0);
            }
            if (loaded.getVersion() != null) {
                update.eq(SysUser::getVersion, loaded.getVersion());
            }
            if (userMapper.update(null, update) == 0) {
                throw new BusinessException(ResultCode.CONFLICT.getCode(),
                        "用户数据已被其他操作修改，请刷新后重试");
            }
        });

        cacheEvictor.evictUserCredentials(user.getId(), user.getUsername());
        log.info("User lifecycle changed: username={}, target={}, by={}",
                user.getUsername(), target, SecurityUtils.getCurrentUsername());
    }

    @Override
    public void forceLogout(UUID id, String reason) {
        log.info("强制用户下线: userId={}, operator={}", id, SecurityUtils.getCurrentUsername());
        authServiceClient.forceLogout(id, reason);
    }

    @Override
    public void updateLastLogin(UUID userId, String ipAddress) {
        updateLastLogin(userId, ipAddress, LocalDateTime.now());
    }

    @Override
    public void updateLastLogin(UUID userId, String ipAddress, LocalDateTime loginTime) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId)
                .set(SysUser::getLastLoginTime, loginTime != null ? loginTime : LocalDateTime.now())
                .set(SysUser::getLastLoginIp, ipAddress);
        userMapper.update(null, wrapper);
    }

    /**
     * 校验当前操作者可以分配目标角色：角色等级不能高于操作者，且角色必须属于当前租户或为平台角色。
     */
    private void validateRoleAssignment(UUID operatorId, List<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Integer operatorMaxRoleLevel = userQueryService.getUserMaxRoleLevel(operatorId);
        for (UUID roleId : roleIds) {
            Integer roleLevel = roleQueryService.getRoleLevel(roleId);
            permissionChecker.requireRoleAssignmentPermission(operatorId, operatorMaxRoleLevel, roleLevel);
            TenantValidationUtil.validateRoleAccess(roleQueryService.getRoleTenantId(roleId));
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private String generateRandomPassword() {
        StringBuilder pwd = new StringBuilder();
        pwd.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(26)));
        pwd.append(PASSWORD_CHARS.charAt(26 + RANDOM.nextInt(26)));
        pwd.append(PASSWORD_CHARS.charAt(52 + RANDOM.nextInt(10)));
        pwd.append(PASSWORD_CHARS.charAt(62 + RANDOM.nextInt(4)));
        for (int i = 0; i < 8; i++) {
            pwd.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        List<Character> chars = pwd.chars().mapToObj(c -> (char) c).collect(java.util.stream.Collectors.toList());
        Collections.shuffle(chars, RANDOM);
        return chars.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining());
    }
}