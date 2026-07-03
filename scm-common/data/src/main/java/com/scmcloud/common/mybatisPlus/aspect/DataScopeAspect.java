package com.scmcloud.common.mybatisPlus.aspect;

import com.scmcloud.common.mybatisPlus.annotation.DataScope;
import com.scmcloud.common.mybatisPlus.context.DataScopeContextHolder;
import com.scmcloud.common.mybatisPlus.context.DataScopeFilter;
import com.scmcloud.common.mybatisPlus.service.DataPermissionService;
import com.scmcloud.common.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 数据权限切面
 * 根据用户角色的dataScope自动注入SQL过滤条件
 *
 * <p>REFACTORED: Now depends on SecurityContext interface instead of concrete SecurityUser class.
 * This follows Dependency Inversion Principle (DIP) and decouples data layer from web layer.
 *
 * @author Deng
 * @since 2025-10-30
 */
@Aspect
@Component
@Slf4j
public class DataScopeAspect {
    private final SecurityContext securityContext;
    private final DataPermissionService dataPermissionService;

    public DataScopeAspect(SecurityContext securityContext, DataPermissionService dataPermissionService) {
        this.securityContext = securityContext;
        this.dataPermissionService = dataPermissionService;
    }

    /**
     * 拦截带有@DataScope注解的方法
     */
    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint point, DataScope dataScope) throws Throwable {
        try {
            if (!securityContext.isAuthenticated()) {
                log.debug("User not authenticated, skipping data scope filtering");
                return point.proceed();
            }

            UUID userId = securityContext.getCurrentUserId();
            UUID deptId = securityContext.getCurrentDeptId();
            Integer dataScopeLevel = securityContext.getDataScopeLevel();

            if (userId == null) {
                log.warn("Authenticated user has null userId, skipping data scope");
                return point.proceed();
            }

            DataScopeFilter filter = buildSqlFilter(dataScopeLevel, userId, deptId, dataScope);
            DataScopeContextHolder.set(filter);

            log.debug("Data scope applied: userId={}, level={}, filter={}",
                    userId, dataScopeLevel, filter.getClause());

            return point.proceed();
        } finally {
            DataScopeContextHolder.clear();
        }
    }

    /**
     * 根据数据权限级别构建SQL过滤条件
     * SECURITY: 校验表别名防止SQL注入
     */
    private DataScopeFilter buildSqlFilter(Integer dataScope, UUID userId, UUID deptId, DataScope annotation) {
        String deptAlias = validateSqlIdentifier(annotation.deptAlias(), "dept_id");
        String userAlias = validateSqlIdentifier(annotation.userAlias(), "create_by");

        return switch (dataScope) {
            case 1 -> // 全部数据权限
                    new DataScopeFilter("1=1", java.util.Collections.emptyMap());

            case 2 -> // 自定义数据权限（从数据库查询配置）
                    buildCustomDataScope(userId, deptAlias, userAlias);

            case 3 -> // 本部门数据权限（PostgreSQL UUID）
                    deptId != null
                            ? new DataScopeFilter(
                                    deptAlias + " = #{__ds_deptId}::uuid",
                                    java.util.Map.of("__ds_deptId", deptId.toString()))
                            : new DataScopeFilter("1=0", java.util.Collections.emptyMap());

            case 4 -> // 本部门及以下数据权限
                    deptId != null
                            ? buildDeptAndChildrenScope(deptId, deptAlias)
                            : new DataScopeFilter("1=0", java.util.Collections.emptyMap());

            case 5 -> // 仅本人数据权限（PostgreSQL UUID）
                    new DataScopeFilter(
                            userAlias + " = #{__ds_userId}::uuid",
                            java.util.Map.of("__ds_userId", userId.toString()));

            default ->
                    new DataScopeFilter("1=0", java.util.Collections.emptyMap()); // 无权限
        };
    }

    /**
     * 构建自定义数据权限
     * 从sys_role_dept表查询用户的自定义权限规则
     */
    private DataScopeFilter buildCustomDataScope(UUID userId, String deptAlias, String userAlias) {
        List<UUID> customDepts = dataPermissionService.findCustomDeptPermissions(userId);

        if (customDepts == null || customDepts.isEmpty()) {
            log.debug("No custom data permission found for user {}, fallback to self only", userId);
            return new DataScopeFilter(
                    userAlias + " = #{__ds_userId}::uuid",
                    java.util.Map.of("__ds_userId", userId.toString())
            );
        }

        Map<String, Object> params = new HashMap<>();
        params.put("__ds_userId", userId.toString());

        // PostgreSQL ANY 语法匹配数组
        String deptList = customDepts.stream()
                .map(UUID::toString)
                .map(s -> "'" + s + "'::uuid")
                .collect(Collectors.joining(","));

        // 组合条件：部门在自定义列表中 OR 本人创建的数据
        String clause = String.format("(%s IN (%s) OR %s = #{__ds_userId}::uuid)",
                deptAlias, deptList, userAlias);

        log.debug("Custom data scope for user {}: {} depts", userId, customDepts.size());
        return new DataScopeFilter(clause, params);
    }

    /**
     * 构建部门及子部门权限
     * 使用PostgreSQL递归CTE查询部门树
     */
    private DataScopeFilter buildDeptAndChildrenScope(UUID deptId, String deptAlias) {
        String clause = """
                %s IN (
                    WITH RECURSIVE dept_tree AS (
                        SELECT id FROM sys_dept WHERE id = #{__ds_deptId}::uuid AND NOT deleted
                        UNION ALL
                        SELECT d.id FROM sys_dept d
                        INNER JOIN dept_tree dt ON d.parent_id = dt.id
                        WHERE NOT d.deleted
                    )
                    SELECT id FROM dept_tree
                )
                """.formatted(deptAlias);
        return new DataScopeFilter(clause, java.util.Map.of("__ds_deptId", deptId.toString()));
    }

    /**
     * 校验SQL标识符（表/列别名），防止SQL注入
     * 仅允许字母、数字、下划线和点（用于限定名）
     */
    private String validateSqlIdentifier(String identifier, String defaultValue) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return defaultValue;
        }

        if (!identifier.matches("^[a-zA-Z0-9_.]+$")) {
            log.error("SECURITY: Invalid SQL identifier in @DataScope annotation: '{}'. " +
                     "Only alphanumeric, underscore, and dot allowed.", identifier);
            throw new IllegalArgumentException(
                "Invalid table/column alias in @DataScope: " + identifier);
        }

        String lower = identifier.toLowerCase(Locale.ROOT);
        String[] forbiddenKeywords = {
            "select", "from", "where", "union", "insert", "update", "delete",
            "drop", "create", "alter", "exec", "execute", "or", "and"
        };

        for (String keyword : forbiddenKeywords) {
            if (lower.equals(keyword)) {
                log.error("SECURITY: SQL keyword used as identifier in @DataScope: '{}'", identifier);
                throw new IllegalArgumentException(
                    "SQL keyword cannot be used as alias in @DataScope: " + identifier);
            }
        }

        return identifier;
    }
}
