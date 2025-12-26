package com.frog.common.security;

import com.frog.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 权限检查工具类
 *
 * 提供用户权限、角色权限、数据权限等检查功能
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionChecker {

    private final PermissionQueryService permissionQueryService;

    /**
     * 检查用户是否有指定权限
     *
     * @param userId 用户 ID
     * @param permissionCode 权限编码
     * @return true=有权限, false=无权限
     */
    public boolean hasPermission(UUID userId, String permissionCode) {
        if (userId == null || permissionCode == null || permissionCode.isEmpty()) {
            return false;
        }

        Set<String> permissions = permissionQueryService.getUserPermissions(userId);
        boolean hasPermission = permissions.contains(permissionCode);

        log.debug("检查用户权限: userId={}, permissionCode={}, result={}", userId, permissionCode, hasPermission);
        return hasPermission;
    }

    /**
     * 检查用户是否有指定角色
     *
     * @param userId 用户 ID
     * @param roleCode 角色编码
     * @return true=有角色, false=无角色
     */
    public boolean hasRole(UUID userId, String roleCode) {
        if (userId == null || roleCode == null || roleCode.isEmpty()) {
            return false;
        }

        Set<String> roles = permissionQueryService.getUserRoles(userId);
        boolean hasRole = roles.contains(roleCode);

        log.debug("检查用户角色: userId={}, roleCode={}, result={}", userId, roleCode, hasRole);
        return hasRole;
    }

    /**
     * 检查用户是否有任一权限
     *
     * @param userId 用户 ID
     * @param permissionCodes 权限编码列表
     * @return true=至少有一个权限, false=无任何权限
     */
    public boolean hasAnyPermission(UUID userId, List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return true;
        }

        Set<String> userPermissions = permissionQueryService.getUserPermissions(userId);
        return permissionCodes.stream().anyMatch(userPermissions::contains);
    }

    /**
     * 检查用户是否有所有权限
     *
     * @param userId 用户 ID
     * @param permissionCodes 权限编码列表
     * @return true=有所有权限, false=缺少某些权限
     */
    public boolean hasAllPermissions(UUID userId, List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return true;
        }

        Set<String> userPermissions = permissionQueryService.getUserPermissions(userId);
        return userPermissions.containsAll(permissionCodes);
    }

    /**
     * 要求用户必须有指定权限，否则抛出异常
     *
     * @param userId 用户 ID
     * @param permissionCode 权限编码
     * @throws BusinessException 如果用户无权限
     */
    public void requirePermission(UUID userId, String permissionCode) {
        if (!hasPermission(userId, permissionCode)) {
            log.warn("权限检查失败: userId={}, permissionCode={}", userId, permissionCode);
            throw new BusinessException("PERMISSION_DENIED", "权限不足：" + permissionCode);
        }
    }

    /**
     * 要求用户必须有指定角色，否则抛出异常
     *
     * @param userId 用户 ID
     * @param roleCode 角色编码
     * @throws BusinessException 如果用户无角色
     */
    public void requireRole(UUID userId, String roleCode) {
        if (!hasRole(userId, roleCode)) {
            log.warn("角色检查失败: userId={}, roleCode={}", userId, roleCode);
            throw new BusinessException("ROLE_REQUIRED", "需要角色：" + roleCode);
        }
    }

    /**
     * 检查用户是否可以访问指定部门的数据
     *
     * @param userId 用户 ID
     * @param userDeptId 用户所属部门 ID
     * @param targetDeptId 目标部门 ID
     * @param dataScope 数据权限范围
     * @param deptPath 部门路径（用于判断上下级关系）
     * @param targetDeptPath 目标部门路径
     * @return true=可以访问, false=不可访问
     */
    public boolean canAccessDepartmentData(UUID userId, UUID userDeptId, UUID targetDeptId,
                                                   String dataScope, String deptPath, String targetDeptPath) {
        // ALL - 可以访问所有部门数据
        if ("ALL".equals(dataScope)) {
            return true;
        }

        // SELF - 只能访问本人创建的数据（不涉及部门）
        if ("SELF".equals(dataScope)) {
            return false; // 需要在业务层判断 create_by
        }

        // DEPT - 只能访问本部门数据
        if ("DEPT".equals(dataScope)) {
            return userDeptId != null && userDeptId.equals(targetDeptId);
        }

        // DEPT_AND_SUB - 可以访问本部门及下级部门数据
        if ("DEPT_AND_SUB".equals(dataScope)) {
            if (userDeptId == null || deptPath == null || targetDeptPath == null) {
                return false;
            }

            // 判断目标部门是否在当前部门的路径下
            return targetDeptPath.startsWith(deptPath);
        }

        // CUSTOM - 自定义规则（需要查询 sys_data_permission_rule）
        if ("CUSTOM".equals(dataScope)) {
            // TODO: 查询自定义数据权限规则
            log.warn("CUSTOM data scope not fully implemented yet for userId: {}", userId);
            return true; // 临时返回 true
        }

        return false;
    }

    /**
     * 检查用户是否可以操作指定资源
     *
     * @param userId 用户 ID
     * @param resourceOwnerId 资源所有者 ID
     * @param resourceDeptId 资源所属部门 ID
     * @param dataScope 数据权限范围
     * @return true=可以操作, false=不可操作
     */
    public boolean canOperateResource(UUID userId, UUID resourceOwnerId, UUID resourceDeptId, String dataScope) {
        // ALL - 可以操作所有资源
        if ("ALL".equals(dataScope)) {
            return true;
        }

        // SELF - 只能操作自己创建的资源
        if ("SELF".equals(dataScope)) {
            return userId.equals(resourceOwnerId);
        }

        // DEPT, DEPT_AND_SUB, CUSTOM - 需要结合部门信息判断
        if ("DEPT".equals(dataScope) || "DEPT_AND_SUB".equals(dataScope) || "CUSTOM".equals(dataScope)) {
            UUID userDeptId = permissionQueryService.getUserDeptId(userId);
            String deptPath = null;
            String targetDeptPath = null;

            if (userDeptId != null) {
                deptPath = permissionQueryService.getDeptPath(userDeptId);
            }

            if (resourceDeptId != null) {
                targetDeptPath = permissionQueryService.getDeptPath(resourceDeptId);
            }

            return canAccessDepartmentData(userId, userDeptId, resourceDeptId, dataScope, deptPath, targetDeptPath);
        }

        return false;
    }

    /**
     * 检查按钮权限
     *
     * @param userId 用户 ID
     * @param buttonCode 按钮权限编码
     * @return true=可见, false=不可见
     */
    public boolean hasButtonPermission(UUID userId, String buttonCode) {
        return hasPermission(userId, buttonCode);
    }

    /**
     * 获取用户的数据权限范围
     *
     * @param userId 用户 ID
     * @return 数据权限范围（ALL, DEPT, DEPT_AND_SUB, SELF, CUSTOM）
     */
    public String getUserDataScope(UUID userId) {
        return permissionQueryService.getUserDataScope(userId);
    }

    /**
     * 获取用户可访问的部门 ID列表
     *
     * @param userId 用户 ID
     * @param tenantId 租户 ID
     * @return 可访问的部门 ID列表
     */
    public List<UUID> getAccessibleDepartmentIds(UUID userId, UUID tenantId) {
        String dataScope = getUserDataScope(userId);
        return permissionQueryService.getAccessibleDepartmentIds(userId, tenantId, dataScope);
    }

    /**
     * 检查用户是否可以分配指定角色
     *
     * @param operatorUserId 操作者用户 ID
     * @param operatorRoleLevel 操作者角色等级
     * @param targetRoleLevel 目标角色等级
     * @return true=可以分配, false=不可分配
     */
    public boolean canAssignRole(UUID operatorUserId, Integer operatorRoleLevel, Integer targetRoleLevel) {
        // 角色等级越大权限越高
        // 只能分配等级不高于自己的角色
        if (operatorRoleLevel == null || targetRoleLevel == null) {
            return false;
        }

        return operatorRoleLevel >= targetRoleLevel;
    }

    /**
     * 要求必须可以分配指定角色，否则抛出异常
     *
     * @param operatorUserId 操作者用户 ID
     * @param operatorRoleLevel 操作者角色等级
     * @param targetRoleLevel 目标角色等级
     * @throws BusinessException 如果不可分配
     */
    public void requireRoleAssignmentPermission(UUID operatorUserId, Integer operatorRoleLevel, Integer targetRoleLevel) {
        if (!canAssignRole(operatorUserId, operatorRoleLevel, targetRoleLevel)) {
            log.warn("角色分配权限不足: operatorUserId={}, operatorLevel={}, targetLevel={}",
                operatorUserId, operatorRoleLevel, targetRoleLevel);
            throw new BusinessException("ROLE_ASSIGNMENT_DENIED", "无权分配该角色（角色等级过高）");
        }
    }
}