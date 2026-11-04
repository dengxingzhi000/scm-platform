package com.cognora.platform.system.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.platform.common.dto.permission.PermissionDTO;
import com.cognora.platform.common.dto.user.UserDTO;
import com.cognora.platform.common.dto.user.UserInfo;
import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.security.PermissionChecker;
import com.cognora.platform.common.tenant.TenantValidationUtil;
import com.cognora.platform.common.web.domain.SecurityUser;
import com.cognora.platform.common.web.util.SecurityUtils;
import com.cognora.platform.system.domain.entity.SysDept;
import com.cognora.platform.system.domain.entity.SysUser;
import com.cognora.platform.system.mapper.SysDeptMapper;
import com.cognora.platform.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final SysDeptMapper deptMapper;
    private final UserCrossDatabaseQueryService userQueryService;
    private final DeptCrossDatabaseQueryService deptQueryService;
    private final PermissionCrossDatabaseQueryService permissionQueryService;
    private final PermissionChecker permissionChecker;

    @Override
    @Slave
    // 不要加 @Transactional：listUsers 会跨 db_user（用户）与 db_permission（数据权限），
    // 事务提前绑定主库连接后 @DS("permission") 无法切换，会报 relation "sys_role" does not exist。
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
        Map<UUID, String> deptNames = loadDeptNames(dtos);
        dtos.forEach(dto -> dto.setDeptName(deptNames.get(dto.getDeptId())));

        Map<UUID, List<Map<String, Object>>> roleRows = userQueryService.findUserRolesWithNamesByUserIds(
                dtos.stream().map(UserDTO::getId).toList());
        for (UserDTO dto : dtos) {
            List<Map<String, Object>> roles = roleRows.get(dto.getId());
            if (roles != null && !roles.isEmpty()) {
                dto.setRoleIds(roles.stream()
                        .map(r -> toUuid(r.get("role_id")))
                        .filter(Objects::nonNull)
                        .toList());
                dto.setRoleNames(roles.stream()
                        .map(r -> String.valueOf(r.get("name")))
                        .toList());
            }
        }

        dtoPage.setRecords(dtos);
        return dtoPage;
    }

    @Override
    @Slave
    @Cacheable(value = "userDetails",
               key = "T(com.cognora.platform.common.tenant.TenantCacheKeys).global(#username)")
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
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectByDeptId(user.getDeptId());
            if (dept != null) {
                dto.setDeptName(dept.getDeptName());
            }
        }
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

    /** 批量补齐部门名称，避免用户列表出现大量 N+1 跨库查询。 */
    private Map<UUID, String> loadDeptNames(List<UserDTO> users) {
        List<UUID> deptIds = users.stream()
                .map(UserDTO::getDeptId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = deptMapper.selectDeptNames(deptIds);
        Map<UUID, String> names = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            Object name = row.get("dept_name");
            if (id == null) {
                continue;
            }
            UUID deptId = id instanceof UUID uuid ? uuid : UUID.fromString(id.toString());
            names.put(deptId, name != null ? name.toString() : null);
        }
        return names;
    }

    private UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }

    private UserDTO convertToDTO(SysUser user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        // 密码哈希绝不能出现在用户列表/详情响应里。
        dto.setPassword(null);
        return dto;
    }
}
