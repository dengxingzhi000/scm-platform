package com.cognora.platform.system.service.Impl;

import com.cognora.platform.common.entity.SysDataPermissionRule;
import com.cognora.platform.common.security.PermissionQueryService;
import com.cognora.platform.system.domain.entity.SysDept;
import com.cognora.platform.system.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 权限查询服务实现
 * <p>
 * 提供权限、角色、数据权限等查询功能，支持缓存
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionQueryServiceImpl implements PermissionQueryService {
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;

    @Override
    @Cacheable(value = "userPermissions", key = "#userId")
    public Set<String> getUserPermissions(UUID userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        Set<String> permissions = userRoleMapper.findPermissionCodesByUserId(userId);
        return permissions != null ? permissions : Collections.emptySet();
    }

    @Override
    @Cacheable(value = "userRoles", key = "#userId")
    public Set<String> getUserRoles(UUID userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        Set<String> roles = userRoleMapper.findRoleCodesByUserId(userId);
        return roles != null ? roles : Collections.emptySet();
    }

    /**
     * 获取用户的数据权限范围（带缓存）
     * <p>
     * 取用户所有角色中权限范围最大的（数值最小的）。数值与 {@code sys_role.data_scope}
     * 的 DDL 约定一致（{@code 003_db_permission.sql}）：
     * 1=ALL, 2=CUSTOM, 3=DEPT, 4=DEPT_AND_SUB, 5=SELF。
     * 该映射必须与 {@link com.cognora.platform.system.service.query.DeptCrossDatabaseQueryService}
     * 和 {@code DataScopeAspect} 保持同一套语义。
     * <p>
     * 缓存键是裸 {@code <userId>}（String 枚举值）。数值码值由
     * {@link com.cognora.platform.system.service.query.UserCrossDatabaseQueryService#getUserDataScope(UUID)}
     * 使用 {@code scope:<userId>} 键缓存，两者刻意不复用同一键，避免“同键不同型”。
     */
    @Override
    @Cacheable(value = "userDataScope", key = "#userId")
    public String getUserDataScope(UUID userId) {
        if (userId == null) {
            return "SELF";
        }

        Integer dataScopeValue = userRoleMapper.getUserDataScope(userId);
        if (dataScopeValue == null) {
            return "SELF";
        }

        return switch (dataScopeValue) {
            case 1 -> "ALL";
            case 2 -> "CUSTOM";
            case 3 -> "DEPT";
            case 4 -> "DEPT_AND_SUB";
            case 5 -> "SELF";
            default -> "SELF";
        };
    }

    @Override
    @Cacheable(value = "userDeptId", key = "#userId")
    public UUID getUserDeptId(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userMapper.getUserDeptId(userId);
    }

    @Override
    @Cacheable(value = "deptPath", key = "#deptId")
    public String getDeptPath(UUID deptId) {
        if (deptId == null) {
            return null;
        }
        SysDept dept = deptMapper.selectById(deptId);
        return dept != null ? dept.getDeptPath() : null;
    }

    /**
     * 获取用户可访问的部门 ID 列表（带缓存）
     */
    @Override
    @Cacheable(value = "accessibleDeptIds", key = "#userId + ':' + #tenantId + ':' + #dataScope")
    public List<UUID> getAccessibleDepartmentIds(UUID userId, UUID tenantId, String dataScope) {
        if (userId == null) {
            return Collections.emptyList();
        }

        return switch (dataScope) {
            case "DEPT" -> {
                UUID userDeptId = getUserDeptId(userId);
                yield userDeptId != null ? List.of(userDeptId) : Collections.emptyList();
            }
            case "DEPT_AND_SUB" -> {
                UUID userDeptId = getUserDeptId(userId);
                if (userDeptId == null) {
                    yield Collections.emptyList();
                }
                List<UUID> deptIds = deptMapper.selectDeptAndChildren(userDeptId);
                yield deptIds != null ? deptIds : List.of(userDeptId);
            }
            case "CUSTOM" -> {
                UUID userDeptId = getUserDeptId(userId);
                log.warn("CUSTOM data scope not implemented yet for userId: {}, tenantId: {}", userId, tenantId);
                yield userDeptId != null ? List.of(userDeptId) : Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }

    @Override
    @Cacheable(value = "roleLevel", key = "#roleId")
    public Integer getRoleLevel(UUID roleId) {
        if (roleId == null) {
            return null;
        }
        return roleMapper.getRoleLevel(roleId);
    }

    /**
     * 获取用户的最高角色等级（带缓存）
     */
    @Override
    @Cacheable(value = "userMaxRoleLevel", key = "#userId")
    public Integer getUserMaxRoleLevel(UUID userId) {
        if (userId == null) {
            return null;
        }

        List<UUID> roleIds = userRoleMapper.findEffectiveRoleIds(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return null;
        }

        Map<UUID, Integer> roleLevels = roleMapper.getRoleLevelsByRoleIds(roleIds);
        return roleLevels.values().stream()
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
    }

    @Override
    public List<SysDataPermissionRule> getCustomDataPermissionRules(UUID userId, String resourceType) {
        if (userId == null) {
            return Collections.emptyList();
        }
        log.warn("getCustomDataPermissionRules not fully implemented for userId: {}, resourceType: {}",
                userId,
                resourceType == null ? null : resourceType.replace("\r", "_").replace("\n", "_"));
        return Collections.emptyList();
    }
}
