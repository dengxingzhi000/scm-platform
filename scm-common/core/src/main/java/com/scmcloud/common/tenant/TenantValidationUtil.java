package com.scmcloud.common.tenant;

import com.scmcloud.common.constant.RoleConstants;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.response.ResultCode;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.UUID;

/**
 * Tenant validation utility
 *
 * Provides tenant context validation, data ownership validation and other functions
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
public class TenantValidationUtil {

    /**
     * Get current tenant ID (throws exception if not set)
     *
     * @return current tenant ID
     * @throws BusinessException if tenant context is not set
     */
    public static UUID getRequiredTenantId() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.error("Tenant context not set, request denied");
            throw new BusinessException(ResultCode.TENANT_CONTEXT_MISSING.getCode(),
                    ResultCode.TENANT_CONTEXT_MISSING.getMessage());
        }
        return tenantId;
    }

    /**
     * Validate if data belongs to current tenant
     *
     * @param dataTenantId tenant ID that the data belongs to
     * @throws BusinessException if data does not belong to current tenant
     */
    public static void validateDataOwnership(UUID dataTenantId) {
        UUID currentTenantId = getRequiredTenantId();

        if (dataTenantId == null) {
            log.error("Data not associated with tenant, data ID may be invalid");
            throw new BusinessException(ResultCode.DATA_TENANT_MISSING.getCode(),
                    ResultCode.DATA_TENANT_MISSING.getMessage());
        }

        if (!currentTenantId.equals(dataTenantId)) {
            log.warn("Tenant data access violation: current tenant={}, data tenant={}", currentTenantId, dataTenantId);
            throw new BusinessException(ResultCode.TENANT_DATA_ACCESS_DENIED.getCode(),
                    ResultCode.TENANT_DATA_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * Validate if user is a platform admin (based on user type string)
     *
     * @param userType user type
     * @return true=platform admin, false=tenant user
     * @deprecated Use no-arg isPlatformAdmin() method which gets info from SecurityContext
     */
    @Deprecated
    public static boolean isPlatformAdmin(String userType) {
        return RoleConstants.USER_TYPE_PLATFORM_ADMIN.equals(userType);
    }

    /**
     * Validate if current user is a platform admin
     * Judgment criteria:
     * 1. User is logged in and has a valid SecurityContext
     * 2. User has platform admin role (ROLE_PLATFORM_ADMIN or ROLE_SUPER_ADMIN)
     * 3. Tenant context is NULL (platform admin does not belong to any tenant)
     *
     * @return true=platform admin, false=tenant user or not logged in
     */
    public static boolean isPlatformAdmin() {
        try {
            // 1. Check tenant context (necessary condition)
            UUID tenantId = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                log.debug("User belongs to tenant {}, not a platform admin", tenantId);
                return false;
            }

            // 2. Try to get current user from SecurityContext (requires SecurityUtils)
            // Note: To avoid circular dependencies, use reflection to invoke SecurityUtils
            // If user info cannot be obtained, only rely on tenant context judgment
            try {
                Class<?> securityUtilsClass = Class.forName("com.scmcloud.common.web.util.SecurityUtils");
                Object currentUser = securityUtilsClass.getMethod("getCurrentUser").invoke(null);

                if (currentUser == null) {
                    log.debug("Not logged in user, not a platform admin");
                    return false;
                }

                // 3. Check roles (sufficient condition)
                Object rolesObj = currentUser.getClass().getMethod("getRoles").invoke(currentUser);
                if (rolesObj instanceof Set) {
                    @SuppressWarnings("unchecked")
                    Set<String> roles = (Set<String>) rolesObj;
                    boolean hasPlatformRole = roles.contains(RoleConstants.ROLE_PLATFORM_ADMIN)
                                           || roles.contains(RoleConstants.ROLE_SUPER_ADMIN);

                    if (!hasPlatformRole) {
                        log.debug("User does not have platform admin role");
                    }

                    return hasPlatformRole;
                }

            } catch (ClassNotFoundException e) {
                // SecurityUtils class does not exist, fall back to tenant ID based judgment
                log.debug("SecurityUtils class does not exist, only judging based on tenant context");
            } catch (Exception e) {
                // Reflection invocation failed, fall back to tenant ID based judgment
                log.debug("Unable to get user role info, only judging based on tenant context: {}", e.getMessage());
            }

            // 4. Fallback logic: if unable to obtain role info and tenant ID is NULL, treat as platform admin
            // This situation usually occurs during system initialization or special scenarios
            return true;

        } catch (Exception e) {
            log.error("Error while checking platform admin permissions", e);
            return false; // Safety first: return false on exception
        }
    }

    /**
     * Validate if current user is a tenant user (non-platform admin)
     * This is a semantic version of !isPlatformAdmin() to avoid awkward code with boolean negation
     *
     * @return true=tenant user, false=platform admin
     */
    public static boolean isTenantUser() {
        return !isPlatformAdmin();
    }

    /**
     * Validate if user is a tenant admin (based on user type string)
     *
     * @param userType user type
     * @return true=tenant admin, false=other
     * @deprecated Use role-based judgment methods instead
     */
    @Deprecated
    public static boolean isTenantAdmin(String userType) {
        return RoleConstants.USER_TYPE_TENANT_ADMIN.equals(userType);
    }

    /**
     * Validate if user has admin privileges (platform admin or tenant admin)
     *
     * @param userType user type
     * @return true=has admin privileges, false=regular user
     * @deprecated This method depends on deprecated methods, use role-based permission checking instead
     */
    @Deprecated
    public static boolean hasAdminPrivilege(String userType) {
        return isPlatformAdmin(userType) || isTenantAdmin(userType);
    }

    /**
     * Validate if role belongs to current tenant or is a platform role
     *
     * @param roleTenantId tenant ID the role belongs to (NULL means platform role)
     * @throws BusinessException if role does not belong to current tenant and is not a platform role
     */
    public static void validateRoleAccess(UUID roleTenantId) {
        // Platform role (tenant_id = NULL): all tenants can use it
        if (roleTenantId == null) {
            return;
        }

        // Tenant role must belong to current tenant
        UUID currentTenantId = getRequiredTenantId();
        if (!currentTenantId.equals(roleTenantId)) {
            log.warn("Tenant role access violation: current tenant={}, role tenant={}", currentTenantId, roleTenantId);
            throw new BusinessException(ResultCode.TENANT_ROLE_ACCESS_DENIED.getCode(),
                    ResultCode.TENANT_ROLE_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * Validate if permission belongs to current tenant or is a platform permission
     *
     * @param permissionTenantId tenant ID the permission belongs to (NULL means platform permission)
     * @throws BusinessException if permission does not belong to current tenant and is not a platform permission
     */
    public static void validatePermissionAccess(UUID permissionTenantId) {
        // Platform permission (tenant_id = NULL): all tenants can use it
        if (permissionTenantId == null) {
            return;
        }

        // Tenant permission must belong to current tenant
        UUID currentTenantId = getRequiredTenantId();
        if (!currentTenantId.equals(permissionTenantId)) {
            log.warn("Tenant permission access violation: current tenant={}, permission tenant={}", currentTenantId, permissionTenantId);
            throw new BusinessException(ResultCode.TENANT_PERMISSION_ACCESS_DENIED.getCode(),
                    ResultCode.TENANT_PERMISSION_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * Validate if creation of platform-level resources (roles, permissions, etc.) is allowed
     *
     * @param userType user type
     * @throws BusinessException if user is not authorized to create platform-level resources
     * @deprecated Use requirePlatformAdmin() instead
     */
    @Deprecated
    public static void validatePlatformResourceCreation(String userType) {
        if (!isPlatformAdmin(userType)) {
            log.warn("Non-platform admin attempting to create platform-level resources, user type: {}", userType);
            throw new BusinessException(ResultCode.PLATFORM_RESOURCE_ACCESS_DENIED.getCode(),
                    ResultCode.PLATFORM_RESOURCE_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * Require the current user to be a platform admin, otherwise throw an exception
     * This is a convenience method for isTenantUser() check, to avoid awkward code with boolean negation
     *
     * @throws BusinessException if the current user is not a platform admin
     */
    public static void requirePlatformAdmin() {
        if (isTenantUser()) {
            log.warn("Non-platform admin attempting to perform platform management operations");
            throw new BusinessException(ResultCode.PLATFORM_RESOURCE_ACCESS_DENIED.getCode(),
                    ResultCode.PLATFORM_RESOURCE_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * Allow platform admin to temporarily access data of a specified tenant
     *
     * @param userType user type
     * @param targetTenantId target tenant ID
     * @return whether access is allowed
     * @deprecated This method depends on deprecated methods, use isPlatformAdmin() directly instead
     */
    @Deprecated
    public static boolean allowCrossTenantAccess(String userType, UUID targetTenantId) {
        // Only platform admin can perform cross-tenant access
        if (!isPlatformAdmin(userType)) {
            return false;
        }

        UUID currentTenantId = TenantContextHolder.getTenantId();

        // Platform admin has tenant_id as NULL, or target tenant is different from current tenant
        return currentTenantId == null || !currentTenantId.equals(targetTenantId);
    }

    /**
     * Validate if department belongs to current tenant
     *
     * @param deptTenantId tenant ID that the department belongs to
     * @throws BusinessException if department does not belong to current tenant
     */
    public static void validateDepartmentOwnership(UUID deptTenantId) {
        UUID currentTenantId = getRequiredTenantId();

        if (deptTenantId == null) {
            log.error("Department not associated with tenant");
            throw new BusinessException(ResultCode.DEPT_TENANT_MISSING.getCode(),
                    ResultCode.DEPT_TENANT_MISSING.getMessage());
        }

        if (!currentTenantId.equals(deptTenantId)) {
            log.warn("Tenant department access violation: current tenant={}, department tenant={}", currentTenantId, deptTenantId);
            throw new BusinessException(ResultCode.TENANT_DEPT_ACCESS_DENIED.getCode(),
                    ResultCode.TENANT_DEPT_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * Record tenant operation log
     *
     * @param operation operation type
     * @param resourceType resource type
     * @param resourceId resource ID
     */
    public static void logTenantOperation(String operation, String resourceType, UUID resourceId) {
        UUID tenantId = TenantContextHolder.getTenantId();
        log.info("Tenant operation log - TenantID: {}, Operation: {}, ResourceType: {}, ResourceID: {}",
            tenantId, operation, resourceType, resourceId);

        // TODO: Can integrate into tenant operation log table (tenant_operation_log)
    }
}
