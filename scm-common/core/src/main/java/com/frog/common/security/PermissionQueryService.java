package com.frog.common.security;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 权限查询服务接口
 *
 * 提供权限、角色、数据权限等查询功能
 *
 * @author Claude Code
 * @since 2025-01-24
 */
public interface PermissionQueryService {

    /**
     * 查询用户的所有权限编码
     *
     * @param userId 用户ID
     * @return 权限编码集合
     */
    Set<String> getUserPermissions(UUID userId);

    /**
     * 查询用户的所有角色编码
     *
     * @param userId 用户ID
     * @return 角色编码集合
     */
    Set<String> getUserRoles(UUID userId);

    /**
     * 获取用户的数据权限范围
     *
     * @param userId 用户ID
     * @return 数据权限范围字符串（ALL, DEPT, DEPT_AND_SUB, SELF, CUSTOM）
     */
    String getUserDataScope(UUID userId);

    /**
     * 获取用户的部门ID
     *
     * @param userId 用户ID
     * @return 部门ID
     */
    UUID getUserDeptId(UUID userId);

    /**
     * 获取部门路径
     *
     * @param deptId 部门ID
     * @return 部门路径
     */
    String getDeptPath(UUID deptId);

    /**
     * 获取用户可访问的部门ID列表
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @param dataScope 数据权限范围
     * @return 可访问的部门ID列表
     */
    List<UUID> getAccessibleDepartmentIds(UUID userId, UUID tenantId, String dataScope);

    /**
     * 获取角色等级
     *
     * @param roleId 角色ID
     * @return 角色等级
     */
    Integer getRoleLevel(UUID roleId);

    /**
     * 获取用户的最高角色等级
     *
     * @param userId 用户ID
     * @return 最高角色等级
     */
    Integer getUserMaxRoleLevel(UUID userId);
}