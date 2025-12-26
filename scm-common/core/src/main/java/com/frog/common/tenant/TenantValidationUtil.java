package com.frog.common.tenant;

import com.frog.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 租户验证工具类
 *
 * 提供租户上下文验证、数据归属验证等功能
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
public class TenantValidationUtil {

    /**
     * 验证当前租户上下文是否已设置
     *
     * @throws BusinessException 如果租户上下文未设置
     */
    public static void validateTenantContext() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.error("租户上下文未设置，请求被拒绝");
            throw new BusinessException("TENANT_CONTEXT_MISSING", "租户上下文未设置，请求被拒绝");
        }
    }

    /**
     * 获取当前租户ID（如果未设置则抛出异常）
     *
     * @return 当前租户ID
     * @throws BusinessException 如果租户上下文未设置
     */
    public static UUID getRequiredTenantId() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.error("租户上下文未设置，请求被拒绝");
            throw new BusinessException("TENANT_CONTEXT_MISSING", "租户上下文未设置，请求被拒绝");
        }
        return tenantId;
    }

    /**
     * 验证数据是否属于当前租户
     *
     * @param dataTenantId 数据所属的租户ID
     * @throws BusinessException 如果数据不属于当前租户
     */
    public static void validateDataOwnership(UUID dataTenantId) {
        UUID currentTenantId = getRequiredTenantId();

        if (dataTenantId == null) {
            log.error("数据未关联租户，数据ID可能无效");
            throw new BusinessException("DATA_TENANT_MISSING", "数据未关联租户");
        }

        if (!currentTenantId.equals(dataTenantId)) {
            log.warn("租户数据访问越权：当前租户={}, 数据租户={}", currentTenantId, dataTenantId);
            throw new BusinessException("TENANT_DATA_ACCESS_DENIED", "无权访问其他租户的数据");
        }
    }

    /**
     * 验证用户是否为平台管理员
     *
     * @param userType 用户类型
     * @return true=平台管理员, false=租户用户
     */
    public static boolean isPlatformAdmin(String userType) {
        return "PLATFORM_ADMIN".equals(userType);
    }

    /**
     * 验证用户是否为租户管理员
     *
     * @param userType 用户类型
     * @return true=租户管理员, false=其他
     */
    public static boolean isTenantAdmin(String userType) {
        return "TENANT_ADMIN".equals(userType);
    }

    /**
     * 验证用户是否有管理权限（平台管理员或租户管理员）
     *
     * @param userType 用户类型
     * @return true=有管理权限, false=普通用户
     */
    public static boolean hasAdminPrivilege(String userType) {
        return isPlatformAdmin(userType) || isTenantAdmin(userType);
    }

    /**
     * 验证角色是否属于当前租户或为平台角色
     *
     * @param roleTenantId 角色所属的租户ID（NULL表示平台角色）
     * @throws BusinessException 如果角色不属于当前租户且不是平台角色
     */
    public static void validateRoleAccess(UUID roleTenantId) {
        // 平台角色（tenant_id = NULL）所有租户都可以使用
        if (roleTenantId == null) {
            return;
        }

        // 租户角色必须属于当前租户
        UUID currentTenantId = getRequiredTenantId();
        if (!currentTenantId.equals(roleTenantId)) {
            log.warn("租户角色访问越权：当前租户={}, 角色租户={}", currentTenantId, roleTenantId);
            throw new BusinessException("TENANT_ROLE_ACCESS_DENIED", "无权访问其他租户的角色");
        }
    }

    /**
     * 验证权限是否属于当前租户或为平台权限
     *
     * @param permissionTenantId 权限所属的租户ID（NULL表示平台权限）
     * @throws BusinessException 如果权限不属于当前租户且不是平台权限
     */
    public static void validatePermissionAccess(UUID permissionTenantId) {
        // 平台权限（tenant_id = NULL）所有租户都可以使用
        if (permissionTenantId == null) {
            return;
        }

        // 租户权限必须属于当前租户
        UUID currentTenantId = getRequiredTenantId();
        if (!currentTenantId.equals(permissionTenantId)) {
            log.warn("租户权限访问越权：当前租户={}, 权限租户={}", currentTenantId, permissionTenantId);
            throw new BusinessException("TENANT_PERMISSION_ACCESS_DENIED", "无权访问其他租户的权限");
        }
    }

    /**
     * 验证是否允许创建平台级资源（角色、权限等）
     *
     * @param userType 用户类型
     * @throws BusinessException 如果用户无权创建平台级资源
     */
    public static void validatePlatformResourceCreation(String userType) {
        if (!isPlatformAdmin(userType)) {
            log.warn("非平台管理员尝试创建平台级资源，用户类型: {}", userType);
            throw new BusinessException("PLATFORM_RESOURCE_ACCESS_DENIED", "只有平台管理员可以创建平台级资源");
        }
    }

    /**
     * 允许平台管理员临时访问指定租户的数据
     *
     * @param userType 用户类型
     * @param targetTenantId 目标租户ID
     * @return 是否允许访问
     */
    public static boolean allowCrossTenantAccess(String userType, UUID targetTenantId) {
        // 只有平台管理员可以跨租户访问
        if (!isPlatformAdmin(userType)) {
            return false;
        }

        UUID currentTenantId = TenantContextHolder.getTenantId();

        // 平台管理员的 tenant_id 为 NULL，或者目标租户与当前租户不同
        return currentTenantId == null || !currentTenantId.equals(targetTenantId);
    }

    /**
     * 验证部门是否属于当前租户
     *
     * @param deptTenantId 部门所属的租户ID
     * @throws BusinessException 如果部门不属于当前租户
     */
    public static void validateDepartmentOwnership(UUID deptTenantId) {
        UUID currentTenantId = getRequiredTenantId();

        if (deptTenantId == null) {
            log.error("部门未关联租户");
            throw new BusinessException("DEPT_TENANT_MISSING", "部门未关联租户");
        }

        if (!currentTenantId.equals(deptTenantId)) {
            log.warn("租户部门访问越权：当前租户={}, 部门租户={}", currentTenantId, deptTenantId);
            throw new BusinessException("TENANT_DEPT_ACCESS_DENIED", "无权访问其他租户的部门");
        }
    }

    /**
     * 记录租户操作日志
     *
     * @param operation 操作类型
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     */
    public static void logTenantOperation(String operation, String resourceType, UUID resourceId) {
        UUID tenantId = TenantContextHolder.getTenantId();
        log.info("租户操作日志 - 租户ID: {}, 操作: {}, 资源类型: {}, 资源ID: {}",
            tenantId, operation, resourceType, resourceId);

        // TODO: 可以集成到租户操作日志表（tenant_operation_log）
    }
}
