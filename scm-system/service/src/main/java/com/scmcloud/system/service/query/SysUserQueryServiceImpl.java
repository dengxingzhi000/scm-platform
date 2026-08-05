package com.scmcloud.system.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.common.dto.permission.PermissionDTO;
import com.scmcloud.common.dto.user.UserDTO;
import com.scmcloud.common.dto.user.UserInfo;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.response.ResultCode;
import com.scmcloud.common.security.PermissionChecker;
import com.scmcloud.common.tenant.TenantValidationUtil;
import com.scmcloud.common.web.domain.SecurityUser;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-side service implementation. All methods route to a slave database via {@link Slave}.
 * Caches (except {@code userDetails}) use the tenant-aware key generator bean
 * {@code tenantAwareCacheKeyGenerator} for tenant isolation. The {@code userDetails}
 * cache stays username-keyed because authentication happens before the tenant context
 * is established.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserQueryServiceImpl implements ISysUserQueryService {

    private static final String KEY_GEN = "tenantAwareCacheKeyGenerator";

    private final SysUserMapper userMapper;
    private final UserCrossDatabaseQueryService userQueryService;
    private final DeptCrossDatabaseQueryService deptQueryService;
    private final PermissionCrossDatabaseQueryService permissionQueryService;
    private final PermissionChecker permissionChecker;

    @Override
    @Slave
    @Transactional(readOnly = true)
    public Page<UserDTO> listUsers(Integer pageNum, Integer pageSize,
                                   String username, Integer status) {
        UUID tenantId = TenantValidationUtil.getRequiredTenantId();
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        String dataScope = permissionChecker.getUserDataScope(operatorId);

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(username != null && !username.isEmpty(), SysUser::getUsername, username)
                .eq(status != null, SysUser::getStatus, status);

        if (!"ALL".equals(dataScope)) {
            List<UUID> accessibleDeptIds =
                    permissionChecker.getAccessibleDepartmentIds(operatorId, tenantId);
            if ("SELF".equals(dataScope)) {
                wrapper.eq(SysUser::getCreateBy, operatorId);
            } else if (!accessibleDeptIds.isEmpty()) {
                wrapper.in(SysUser::getDeptId, accessibleDeptIds);
            } else {
                return new Page<>(pageNum, pageSize, 0);
            }
        }

        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> userPage = userMapper.selectPage(page, wrapper);

        Page<UserDTO> dtoPage = new Page<>(pageNum, pageSize, userPage.getTotal());
        List<UserDTO> dtos = userPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtos);
        return dtoPage;
    }

    @Override
    @Slave
    @Cacheable(value = "userDetails", key = "#username")
    public SecurityUser getUserByUsername(String username) {
        SysUser user = userMapper.findByUsername(username);
        if (user == null) {
            return null;
        }
        Set<String> roles = userQueryService.findRoleCodesByUserId(user.getId());
        Set<String> permissions = userQueryService.findPermissionCodesByUserId(user.getId());
        return SecurityUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .realName(user.getRealName())
                .deptId(user.getDeptId())
                .status(user.getStatus())
                .accountType(user.getAccountType())
                .userLevel(user.getUserLevel())
                .roles(roles != null ? roles : Collections.emptySet())
                .permissions(permissions != null ? permissions : Collections.emptySet())
                .twoFactorSecret(user.getTwoFactorSecret())
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .passwordExpireTime(user.getPasswordExpireTime())
                .forceChangePassword(user.getForceChangePassword())
                .build();
    }

    @Override
    @Slave
    @Cacheable(value = "user", keyGenerator = KEY_GEN)
    public UserDTO getUserById(UUID id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }
        UserDTO dto = convertToDTO(user);
        List<Map<String, Object>> roles = userQueryService.findUserRolesWithNames(id);
        if (!roles.isEmpty()) {
            List<UUID> roleIds = roles.stream()
                    .map(r -> (UUID) r.get("id"))
                    .toList();
            List<String> roleNames = roles.stream()
                    .map(r -> (String) r.get("name"))
                    .toList();
            dto.setRoleIds(roleIds);
            dto.setRoleNames(roleNames);
        }
        return dto;
    }

    @Override
    @Slave
    @Cacheable(value = "userInfo", keyGenerator = KEY_GEN)
    public UserInfo getUserInfo(UUID userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }
        UserInfo info = UserInfo.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .deptId(user.getDeptId())
                .userLevel(user.getUserLevel())
                .build();

        Set<String> roles = userQueryService.findRoleCodesByUserId(userId);
        Set<String> permissions = userQueryService.findPermissionCodesByUserId(userId);
        info.setRoles(roles);
        info.setPermissions(permissions);

        List<PermissionDTO> menuTree = permissionQueryService.findMenuTreeByUserId(userId);
        info.setMenuTree(new HashSet<>(menuTree));
        return info;
    }

    @Override
    @Slave
    @Cacheable(value = "userTemporaryRoles", keyGenerator = KEY_GEN, unless = "#result == null || #result.isEmpty()")
    public List<Map<String, Object>> getUserTemporaryRoles(UUID userId) {
        return userQueryService.findTemporaryRolesByUserId(userId);
    }

    @Override
    @Slave
    public boolean canAccessDept(UUID userId, UUID deptId) {
        return deptQueryService.hasAccessToDept(userId, deptId);
    }

    @Override
    @Slave
    @Cacheable(value = "userDataScope", keyGenerator = KEY_GEN)
    public Integer getUserDataScope(UUID userId) {
        return userQueryService.getUserDataScope(userId);
    }

    @Override
    @Slave
    public Map<String, Object> getUserStatistics(UUID userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("roleCount", userQueryService.countUserRoles(userId));
        stats.put("temporaryRoleCount", userQueryService.countTemporaryRoles(userId));
        stats.put("expiringRoleCount", userQueryService.countExpiringRoles(userId, 7));
        stats.put("dataScope", userQueryService.getUserDataScope(userId));
        stats.put("maxApprovalAmount", userQueryService.getMaxApprovalAmount(userId));
        return stats;
    }

    private UserDTO convertToDTO(SysUser user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}
