package com.scmcloud.system.service.command;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.scmcloud.common.dto.user.UserDTO;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.response.ResultCode;
import com.scmcloud.common.security.PermissionChecker;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.common.tenant.TenantValidationUtil;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.domain.enums.UserStatus;
import com.scmcloud.system.event.DataSyncEventPublisher;
import com.scmcloud.system.mapper.SysUserMapper;
import com.scmcloud.system.service.query.RoleCrossDatabaseQueryService;
import com.scmcloud.system.service.query.UserCrossDatabaseQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String KEY_GEN = "tenantAwareCacheKeyGenerator";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";

    private final SysUserMapper userMapper;
    private final UserCrossDatabaseQueryService userQueryService;
    private final RoleCrossDatabaseQueryService roleQueryService;
    private final UserRoleCrossDatabaseCommandService userRoleCommandService;
    private final PasswordEncoder passwordEncoder;
    private final DataSyncEventPublisher dataSyncEventPublisher;
    private final PermissionChecker permissionChecker;
    private final StatusValidator statusValidator;
    private final UserCommandTemplate commandTemplate;

    @Value("${app.security.admin-user-id:019a0aee-3b74-7bfc-b34f-48b5428d4875}")
    private String adminUserId;

    @Value("${spring.security.default-password}")
    private String defaultPassword;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, allEntries = true)
    public void addUser(UserDTO userDTO) {
        UUID tenantId = TenantValidationUtil.getRequiredTenantId();
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "user:add");

        if (userMapper.existsByUsername(userDTO.getUsername())) {
            throw new BusinessException(ResultCode.USER_EXIST.getCode(),
                    ResultCode.USER_EXIST.getMessage());
        }

        String encoded = (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty())
                ? passwordEncoder.encode(userDTO.getPassword())
                : passwordEncoder.encode(defaultPassword);

        SysUser user = new SysUser();
        BeanUtils.copyProperties(userDTO, user);
        user.setPassword(encoded);
        user.setId(UUIDv7Util.generate());
        user.setTenantId(tenantId);
        user.setPasswordExpireTime(LocalDateTime.now().plusDays(90));
        user.setForceChangePassword(true);

        userMapper.insert(user);

        if (userDTO.getRoleIds() != null && !userDTO.getRoleIds().isEmpty()) {
            int inserted = userRoleCommandService.batchInsertUserRoles(
                    user.getId(), userDTO.getRoleIds(), operatorId);
            log.debug("创建用户时分配角色: user={}, roleCount={}", user.getUsername(), inserted);
        }

        dataSyncEventPublisher.publishUserCreated(user);
        TenantValidationUtil.logTenantOperation("CREATE", "USER", user.getId());

        log.info("用户创建成功: username={}, operator={}", user.getUsername(),
                SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, keyGenerator = KEY_GEN)
    public void updateUser(UserDTO userDTO) {
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        SysUser user = commandTemplate.execute("UPDATE", "user:update", userDTO.getId(),
                (loaded, op) -> {
                    SysUser update = new SysUser();
                    BeanUtils.copyProperties(userDTO, update);
                    update.setPassword(null);
                    userMapper.updateById(update);

                    if (userDTO.getRoleIds() != null) {
                        int deleted = userRoleCommandService.deleteUserRoles(loaded.getId());
                        log.debug("更新用户时清除旧角色: user={}, deletedCount={}",
                                loaded.getUsername(), deleted);
                        if (!userDTO.getRoleIds().isEmpty()) {
                            int inserted = userRoleCommandService.batchInsertUserRoles(
                                    loaded.getId(), userDTO.getRoleIds(), op);
                            log.debug("更新用户时重新分配角色: user={}, newRoleCount={}",
                                    loaded.getUsername(), inserted);
                        }
                    }
                });

        SysUser updated = userMapper.selectById(user.getId());
        dataSyncEventPublisher.publishUserUpdated(updated);

        log.info("用户更新成功: username={}, operator={}", user.getUsername(),
                SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, keyGenerator = KEY_GEN)
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
                    userMapper.deleteById(loaded.getId());
                    dataSyncEventPublisher.publishUserDeleted(loaded.getId());
                });

        log.info("用户删除成功: username={}, operator={}", user.getUsername(),
                SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails"}, keyGenerator = KEY_GEN)
    public String resetPassword(UUID id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }
        TenantValidationUtil.validateDataOwnership(user.getTenantId());

        String newPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setForceChangePassword(true);
        userMapper.updateById(user);

        log.info("Password reset for user: {}, by: {}",
                user.getUsername(), SecurityUtils.getCurrentUsername());
        return newPassword;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails"}, keyGenerator = KEY_GEN)
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
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

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setForceChangePassword(false);
        user.setLastPasswordChangeTime(LocalDateTime.now());
        user.setPasswordExpireTime(LocalDateTime.now().plusDays(90));
        userMapper.updateById(user);

        log.info("Password changed for user: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, keyGenerator = KEY_GEN)
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

        if (roleIds != null && !roleIds.isEmpty()) {
            Integer operatorMaxRoleLevel = userQueryService.getUserMaxRoleLevel(operatorId);
            for (UUID roleId : roleIds) {
                Integer roleLevel = roleQueryService.getRoleLevel(roleId);
                permissionChecker.requireRoleAssignmentPermission(
                        operatorId, operatorMaxRoleLevel, roleLevel);

                UUID roleTenantId = roleQueryService.getRoleTenantId(roleId);
                TenantValidationUtil.validateRoleAccess(roleTenantId);
            }
        }

        int deleted = userRoleCommandService.deleteUserRoles(user.getId());
        log.debug("授权操作清除原有角色: user={}, deletedCount={}",
                user.getUsername(), deleted);
        if (roleIds != null && !roleIds.isEmpty()) {
            int inserted = userRoleCommandService.batchInsertUserRoles(
                    user.getId(), roleIds, operatorId);
            log.debug("授权操作分配新角色: user={}, grantedCount={}",
                    user.getUsername(), inserted);
        }

        TenantValidationUtil.logTenantOperation("GRANT_ROLES", "USER", userId);

        log.info("角色授予操作完成: user={}, roleIds={}, operator={}",
                user.getUsername(), roleIds, SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails"}, keyGenerator = KEY_GEN)
    public void lockUser(UUID id, Boolean lock) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }

        UserStatus fromStatus = UserStatus.fromCode(user.getStatus());
        if (lock) {
            statusValidator.validateTransition("USER",
                    fromStatus != null ? fromStatus.name() : String.valueOf(user.getStatus()),
                    "LOCKED");
            user.setStatus(UserStatus.LOCKED.getCode());
            user.setLockedUntil(LocalDateTime.now().plusHours(24));
        } else {
            statusValidator.validateTransition("USER",
                    fromStatus != null ? fromStatus.name() : String.valueOf(user.getStatus()),
                    "ACTIVE");
            user.setStatus(UserStatus.ACTIVE.getCode());
            user.setLockedUntil(null);
            user.setLoginAttempts(0);
        }
        userMapper.updateById(user);

        log.info("User {} {}, by: {}",
                user.getUsername(), lock ? "locked" : "unlocked",
                SecurityUtils.getCurrentUsername());
    }

    @Override
    public void updateLastLogin(UUID userId, String ipAddress) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId)
                .set(SysUser::getLastLoginTime, LocalDateTime.now())
                .set(SysUser::getLastLoginIp, ipAddress);
        userMapper.update(null, wrapper);
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