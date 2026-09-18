package com.scmcloud.common.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.scmcloud.common.entity.SysDataPermissionRule;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.response.ResultCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Permission check service with Caffeine L1 cache.
 *
 * <p>Provides user permission, role, and data permission checks.</p>
 *
 * @since 2025-01-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionChecker {

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final int CACHE_MAX_SIZE = 10_000;

    private final PermissionQueryService permissionQueryService;
    private final ObjectMapper objectMapper;

    private final Cache<UUID, Set<String>> userPermissionsCache = Caffeine.newBuilder()
            .maximumSize(CACHE_MAX_SIZE)
            .expireAfterWrite(CACHE_TTL)
            .build();

    private final Cache<UUID, Set<String>> userRolesCache = Caffeine.newBuilder()
            .maximumSize(CACHE_MAX_SIZE)
            .expireAfterWrite(CACHE_TTL)
            .build();

    public boolean hasPermission(UUID userId, String permissionCode) {
        if (userId == null || permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        Set<String> permissions = userPermissionsCache.get(userId,
                k -> permissionQueryService.getUserPermissions(k));
        boolean has = permissions.contains(permissionCode);
        log.debug("hasPermission: userId={}, code={}, result={}", userId, permissionCode, has);
        return has;
    }

    public boolean hasRole(UUID userId, String roleCode) {
        if (userId == null || roleCode == null || roleCode.isBlank()) {
            return false;
        }
        Set<String> roles = userRolesCache.get(userId,
                k -> permissionQueryService.getUserRoles(k));
        boolean has = roles.contains(roleCode);
        log.debug("hasRole: userId={}, code={}, result={}", userId, roleCode, has);
        return has;
    }

    public boolean hasAnyPermission(UUID userId, List<String> permissionCodes) {
        if (userId == null || permissionCodes == null || permissionCodes.isEmpty()) {
            return false;
        }
        Set<String> userPermissions = userPermissionsCache.get(userId,
                k -> permissionQueryService.getUserPermissions(k));
        return permissionCodes.stream().anyMatch(userPermissions::contains);
    }

    public boolean hasAllPermissions(UUID userId, List<String> permissionCodes) {
        if (userId == null || permissionCodes == null || permissionCodes.isEmpty()) {
            return false;
        }
        Set<String> userPermissions = userPermissionsCache.get(userId,
                k -> permissionQueryService.getUserPermissions(k));
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
            throw new BusinessException(ResultCode.PERMISSION_DENIED.getCode(), "权限不足: " + permissionCode);
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
            throw new BusinessException(ResultCode.ROLE_REQUIRED.getCode(), "需要角色：" + roleCode);
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
        if ("ALL".equals(dataScope)) {
            return true;
        }

        if ("SELF".equals(dataScope)) {
            return false; // 需要在业务层判断 create_by
        }

        if ("DEPT".equals(dataScope)) {
            return userDeptId != null && userDeptId.equals(targetDeptId);
        }

        if ("DEPT_AND_SUB".equals(dataScope)) {
            if (userDeptId == null || deptPath == null || targetDeptPath == null) {
                return false;
            }
            return targetDeptPath.startsWith(deptPath);
        }

        if ("CUSTOM".equals(dataScope)) {
            return checkCustomDataPermission(userId, targetDeptId);
        }

        return false;
    }

    /**
     * 检查用户是否无权操作指定资源
     *
     * @param userId 用户 ID
     * @param resourceOwnerId 资源所有者 ID
     * @param resourceDeptId 资源所属部门 ID
     * @param dataScope 数据权限范围
     * @return true=无权操作, false=可以操作
     */
    public boolean cannotOperateResource(UUID userId, UUID resourceOwnerId, UUID resourceDeptId, String dataScope) {
        return !canOperateResourceInternal(userId, resourceOwnerId, resourceDeptId, dataScope);
    }

    private boolean canOperateResourceInternal(UUID userId, UUID resourceOwnerId, UUID resourceDeptId, String dataScope) {
        if ("ALL".equals(dataScope)) {
            return true;
        }

        if ("SELF".equals(dataScope)) {
            return userId.equals(resourceOwnerId);
        }

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
     * 获取用户可访问的部门 ID 列表
     *
     * @param userId 用户 ID
     * @param tenantId 租户 ID
     * @return 可访问的部门 ID 列表
     */
    public List<UUID> getAccessibleDepartmentIds(UUID userId, UUID tenantId) {
        String dataScope = getUserDataScope(userId);
        return permissionQueryService.getAccessibleDepartmentIds(userId, tenantId, dataScope);
    }

    /**
     * 检查用户是否可以分配指定角色
     *
     * @param operatorUserId 操作者用户 ID（当前未使用，保留用于审计）
     * @param operatorRoleLevel 操作者角色等级
     * @param targetRoleLevel 目标角色等级
     * @return true=可以分配, false=不可分配
     */
    public boolean canAssignRole(UUID operatorUserId, Integer operatorRoleLevel, Integer targetRoleLevel) {
        if (operatorRoleLevel == null || targetRoleLevel == null) {
            return false;
        }
        return operatorRoleLevel >= targetRoleLevel;
    }

    /**
     * 要求必须可以分配指定角色，否则抛出异常
     *
     * @param operatorUserId 操作者用户 ID（当前未使用，保留用于审计）
     * @param operatorRoleLevel 操作者角色等级
     * @param targetRoleLevel 目标角色等级
     * @throws BusinessException 如果不可分配
     */
    public void requireRoleAssignmentPermission(UUID operatorUserId, Integer operatorRoleLevel, Integer targetRoleLevel) {
        if (!canAssignRole(operatorUserId, operatorRoleLevel, targetRoleLevel)) {
            log.warn("角色分配权限不足: operatorUserId={}, operatorLevel={}, targetLevel={}",
                operatorUserId, operatorRoleLevel, targetRoleLevel);
            throw new BusinessException(ResultCode.ROLE_ASSIGNMENT_DENIED.getCode(), ResultCode.ROLE_ASSIGNMENT_DENIED.getMessage());
        }
    }

    /**
     * 检查自定义数据权限规则
     *
     * @param userId 用户 ID
     * @param targetDeptId 目标部门 ID
     * @return true=允许访问, false=拒绝访问
     */
    private boolean checkCustomDataPermission(UUID userId, UUID targetDeptId) {
        List<SysDataPermissionRule> rules = permissionQueryService.getCustomDataPermissionRules(userId, null);
        if (rules == null || rules.isEmpty()) {
            return false;
        }

        for (SysDataPermissionRule rule : rules) {
            if (!Boolean.TRUE.equals(rule.getEnabled())) {
                continue;
            }

            try {
                List<UUID> allowedDeptIds = objectMapper.readValue(
                        rule.getRuleValue(),
                        new TypeReference<>() {}
                );

                if ("INCLUDE".equals(rule.getRuleType())) {
                    if (targetDeptId != null && allowedDeptIds.contains(targetDeptId)) {
                        return true;
                    }
                } else if ("EXCLUDE".equals(rule.getRuleType())) {
                    if (targetDeptId != null && !allowedDeptIds.contains(targetDeptId)) {
                        return true;
                    }
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse custom data permission rule: ruleId={}, error={}", rule.getId(), e.getMessage());
            }
        }

        return false;
    }
}
