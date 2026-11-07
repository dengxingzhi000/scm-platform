package com.cognora.platform.system.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.platform.common.dto.permission.PermissionDTO;
import com.cognora.platform.common.dto.user.UserDTO;
import com.cognora.platform.common.dto.user.UserDetailDTO;
import com.cognora.platform.common.dto.user.UserInfo;
import com.cognora.platform.common.dto.user.UserQueryRequest;
import com.cognora.platform.common.dto.user.UserSummaryDTO;
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
import com.cognora.platform.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private final SysUserRoleMapper userRoleMapper;
    private final UserCrossDatabaseQueryService userQueryService;
    private final DeptCrossDatabaseQueryService deptQueryService;
    private final PermissionCrossDatabaseQueryService permissionQueryService;
    private final PermissionChecker permissionChecker;

    @Override
    @Slave
    // 不要加 @Transactional：listUsers 会跨 db_user（用户）与 db_permission（数据权限），
    // 事务提前绑定主库连接后 @DS("permission") 无法切换，会报 relation "sys_role" does not exist。
    public Page<UserSummaryDTO> listUsers(UserQueryRequest request) {
        UUID tenantId = TenantValidationUtil.getRequiredTenantId();
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        String dataScope = permissionChecker.getUserDataScope(operatorId);

        int pageNum = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
        int pageSize = request.getSize() != null ? Math.min(request.getSize(), 100) : 10;
        Page<SysUser> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(request.getUsername() != null && !request.getUsername().isEmpty(),
                        SysUser::getUsername, request.getUsername())
                .like(request.getRealName() != null && !request.getRealName().isEmpty(),
                        SysUser::getRealName, request.getRealName())
                .like(request.getPhone() != null && !request.getPhone().isEmpty(),
                        SysUser::getPhone, request.getPhone())
                .like(request.getEmail() != null && !request.getEmail().isEmpty(),
                        SysUser::getEmail, request.getEmail())
                .eq(request.getDeptId() != null, SysUser::getDeptId, request.getDeptId())
                .eq(request.getStatus() != null, SysUser::getStatus, request.getStatus())
                .ge(request.getCreateTimeFrom() != null, SysUser::getCreateTime, request.getCreateTimeFrom())
                .le(request.getCreateTimeTo() != null, SysUser::getCreateTime, request.getCreateTimeTo());

        if (request.getRoleId() != null) {
            List<UUID> roleUserIds = userRoleMapper.findUserIdsByRoleId(request.getRoleId());
            if (roleUserIds == null || roleUserIds.isEmpty()) {
                return new Page<>(pageNum, pageSize, 0);
            }
            wrapper.in(SysUser::getId, roleUserIds);
        }

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

        applySort(wrapper, request.getSortBy(), request.getSortOrder());

        Page<SysUser> userPage = userMapper.selectPage(page, wrapper);

        Page<UserSummaryDTO> dtoPage = new Page<>(pageNum, pageSize, userPage.getTotal());
        List<UserSummaryDTO> dtos = userPage.getRecords().stream()
                .map(this::convertToSummary)
                .collect(Collectors.toList());
        Map<UUID, String> deptNames = loadDeptNames(dtos);
        dtos.forEach(dto -> dto.setDeptName(deptNames.get(dto.getDeptId())));

        Map<UUID, List<Map<String, Object>>> roleRows = userQueryService.findUserRolesWithNamesByUserIds(
                dtos.stream().map(UserSummaryDTO::getId).toList());
        for (UserSummaryDTO dto : dtos) {
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
    public UserDetailDTO getUserDetail(UUID id) {
        // 这里直接复用 getUserById 的聚合结果；自调用不会命中 Spring Cache，
        // 但详情接口前面已经由 UserReadAccessGuard 做过数据范围校验。
        UserDTO dto = getUserById(id);
        UserDetailDTO detail = new UserDetailDTO();
        BeanUtils.copyProperties(dto, detail);
        detail.setPermissions(new ArrayList<>(userQueryService.findPermissionCodesByUserId(id)));
        return detail;
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
    private Map<UUID, String> loadDeptNames(List<UserSummaryDTO> users) {
        List<UUID> deptIds = users.stream()
                .map(UserSummaryDTO::getDeptId)
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

    private UserSummaryDTO convertToSummary(SysUser user) {
        UserSummaryDTO dto = new UserSummaryDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }

    /**
     * 排序字段只接受白名单值；非法值在 DTO 校验阶段已拒绝，这里保留默认排序兜底。
     */
    private void applySort(LambdaQueryWrapper<SysUser> wrapper, String sortBy, String sortOrder) {
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        String field = sortBy == null || sortBy.isEmpty() ? "createTime" : sortBy;
        switch (field) {
            case "username" -> wrapper.orderBy(true, asc, SysUser::getUsername);
            case "realName" -> wrapper.orderBy(true, asc, SysUser::getRealName);
            case "status" -> wrapper.orderBy(true, asc, SysUser::getStatus);
            case "lastLoginTime" -> wrapper.orderBy(true, asc, SysUser::getLastLoginTime);
            case "createTime" -> wrapper.orderBy(true, asc, SysUser::getCreateTime);
            default -> wrapper.orderByDesc(SysUser::getCreateTime);
        }
    }
}
