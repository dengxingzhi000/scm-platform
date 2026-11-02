package com.scmcloud.system.cache;

import com.scmcloud.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 用户/角色/权限缓存的统一失效组件。
 *
 * <p>所有键格式都集中在这里，避免各个 Command Service 各自复刻
 * {@code TenantAwareCacheKeyGenerator} 而逐渐漂移。键格式已按真实
 * {@code @Cacheable} 定义核对（见测试 {@code UserAuthorizationCacheEvictorTest}）：</p>
 *
 * <ul>
 *   <li>{@code user}/{@code userInfo}/{@code userDataScope}/{@code userTemporaryRoles}
 *       的部分实现使用 {@code tenantAwareCacheKeyGenerator}，逻辑键为
 *       {@code <tenantId>:<userId>}（无租户时为 {@code no-tenant:<userId>}）；</li>
 *   <li>{@code userRoles}/{@code userRoleCodes}/{@code userPermissionCodes}/
 *       {@code userRolesWithNames}/{@code userMaxRoleLevel}/{@code userMenuTree}/
 *       {@code userDeptId}，以及 {@code userTemporaryRoles} 的另一套实现，
 *       逻辑键为裸 {@code <userId>}；</li>
 *   <li>{@code userDataScope} 有三个互不冲突的键：裸 {@code <userId>} 是
 *       {@code PermissionQueryServiceImpl} 缓存的 String 枚举，{@code scope:<userId>}
 *       是 {@code UserCrossDatabaseQueryService} 缓存的 Integer 码值，
 *       {@code <tenantId>:<userId>} 是 {@code SysUserQueryServiceImpl} 的 KEY_GEN 键；</li>
 *   <li>{@code userDetails} 逻辑键为裸 {@code <username>}；</li>
 *   <li>{@code userPermissions} 同时存在 {@code <userId>} 和 {@code <userId>:<permissionCode>}
 *       两种键，{@code accessibleDeptIds} 是 {@code <userId>:<tenantId>:<dataScope>} 复合键，
 *       Cache 接口无法按前缀精确清理，因此整体 {@code clear()}；</li>
 *   <li>{@code role}/{@code rolePermissions}/{@code roleLevel}/{@code permission} 的键是 UUID 对象；
 *       {@code permissionTree}/{@code apiPermissions} 的键是字面量 {@code all}；
 *       {@code roles} 的键含方法名和租户，无法枚举，整体 {@code clear()}。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuthorizationCacheEvictor {

    private static final String USER = "user";
    private static final String USER_INFO = "userInfo";
    private static final String USER_DETAILS = "userDetails";
    private static final String USER_ROLES = "userRoles";
    private static final String USER_PERMISSIONS = "userPermissions";
    private static final String USER_ROLE_CODES = "userRoleCodes";
    private static final String USER_PERMISSION_CODES = "userPermissionCodes";
    private static final String USER_ROLES_WITH_NAMES = "userRolesWithNames";
    private static final String USER_DATA_SCOPE = "userDataScope";
    private static final String USER_MAX_ROLE_LEVEL = "userMaxRoleLevel";
    private static final String USER_TEMPORARY_ROLES = "userTemporaryRoles";
    private static final String USER_MENU_TREE = "userMenuTree";
    private static final String USER_DEPT_ID = "userDeptId";
    private static final String ACCESSIBLE_DEPT_IDS = "accessibleDeptIds";

    private static final String ROLE = "role";
    private static final String ROLES = "roles";
    private static final String ROLE_PERMISSIONS = "rolePermissions";
    private static final String ROLE_LEVEL = "roleLevel";

    private static final String PERMISSION = "permission";
    private static final String PERMISSION_TREE = "permissionTree";
    private static final String API_PERMISSIONS = "apiPermissions";

    private final CacheManager cacheManager;

    /**
     * 只失效身份/凭证缓存：{@code user} + {@code userDetails}。
     * 适用于改密、重置密码、锁定/解锁等不改变角色/权限/部门的写操作。
     */
    public void evictUserCredentials(UUID userId, String username) {
        evictTenantKeyed(USER, userId);
        evictUserDetails(username);
    }

    /**
     * 失效用户资料缓存：{@code user} + {@code userInfo} + {@code userDetails}。
     */
    public void evictUserProfile(UUID userId, String username) {
        evictTenantKeyed(USER, userId);
        evictTenantKeyed(USER_INFO, userId);
        evictUserDetails(username);
    }

    /**
     * 失效用户全部授权相关缓存（角色授予/撤销、临时角色变更、删除用户等）。
     *
     * <p>其中 {@code userPermissions} 与 {@code accessibleDeptIds} 是复合键，
     * 无法用 {@link Cache#evict(Object)} 精确清理，只能整体清理。</p>
     */
    public void evictUserAuthorization(UUID userId, String username) {
        evictUserProfile(userId, username);

        evictBare(USER_ROLES, userId);
        evictBare(USER_ROLE_CODES, userId);
        evictBare(USER_PERMISSION_CODES, userId);
        evictBare(USER_ROLES_WITH_NAMES, userId);
        evictBare(USER_MAX_ROLE_LEVEL, userId);
        evictBare(USER_MENU_TREE, userId);

        // userDataScope 的三个键形态都要失效，缺一不可：
        // 裸 userId（String 枚举）、scope:userId（Integer 码值）、tenantId:userId（KEY_GEN）。
        evictBare(USER_DATA_SCOPE, userId);
        evictScopeCode(USER_DATA_SCOPE, userId);
        evictTenantKeyed(USER_DATA_SCOPE, userId);

        // userTemporaryRoles 同样存在 KEY_GEN 与裸 userId 两套键。
        evictBare(USER_TEMPORARY_ROLES, userId);
        evictTenantKeyed(USER_TEMPORARY_ROLES, userId);

        clear(USER_PERMISSIONS);
        clear(ACCESSIBLE_DEPT_IDS);
    }

    /**
     * 用户部门变更：{@code userDeptId} 精确失效，{@code accessibleDeptIds} 复合键整体清理。
     */
    public void evictUserDept(UUID userId) {
        evictBare(USER_DEPT_ID, userId);
        clear(ACCESSIBLE_DEPT_IDS);
    }

    /**
     * 角色/权限定义级别的写操作（角色更新、角色权限授予、权限定义变更），
     * 受影响用户集合无法低成本枚举，因此整体清理用户授权缓存。
     */
    public void evictAllUserAuthorization() {
        clear(USER);
        clear(USER_INFO);
        clear(USER_DETAILS);
        clear(USER_ROLES);
        clear(USER_PERMISSIONS);
        clear(USER_ROLE_CODES);
        clear(USER_PERMISSION_CODES);
        clear(USER_ROLES_WITH_NAMES);
        clear(USER_DATA_SCOPE);
        clear(USER_MAX_ROLE_LEVEL);
        clear(USER_TEMPORARY_ROLES);
        clear(USER_MENU_TREE);
        clear(ACCESSIBLE_DEPT_IDS);
    }

    /** 角色列表缓存（键含方法名和租户，无法枚举）。 */
    public void evictRoleList() {
        clear(ROLES);
    }

    /**
     * 角色定义变更/删除：按 roleId 精确失效 role/rolePermissions/roleLevel，roles 列表整体清理。
     */
    public void evictRoleDefinition(UUID roleId) {
        if (roleId != null) {
            evictKey(ROLE, roleId);
            evictKey(ROLE_PERMISSIONS, roleId);
            evictKey(ROLE_LEVEL, roleId);
        }
        clear(ROLES);
    }

    /**
     * 角色权限变更：role 与 rolePermissions 按 roleId 精确失效；
     * 用户权限相关缓存受影响用户未知，整体清理。
     */
    public void evictRolePermissionGrant(UUID roleId) {
        if (roleId != null) {
            evictKey(ROLE, roleId);
            evictKey(ROLE_PERMISSIONS, roleId);
        }
        clear(USER_PERMISSIONS);
        clear(USER_PERMISSION_CODES);
        clear(USER_DETAILS);
        clear(USER_MENU_TREE);
        clear(USER_INFO);
        clear(USER);
    }

    /**
     * 权限定义新增/删除：permission 按 id 精确失效，permissionTree/apiPermissions 键为 all。
     * 新增/删除权限不允许存在角色或临时权限引用，因此不影响已有用户权限。
     */
    public void evictPermissionDefinition(UUID permissionId) {
        if (permissionId != null) {
            evictKey(PERMISSION, permissionId);
        }
        // 两级缓存模式下 permissionTree/apiPermissions 的实际键会带租户前缀；
        // clear() 由缓存实现按当前租户处理（平台管理员无租户时清全部），比 evict("all") 更正确。
        clear(PERMISSION_TREE);
        clear(API_PERMISSIONS);
    }

    /**
     * 权限定义修改：可能改变已有用户的权限编码/状态，额外清理用户权限相关缓存。
     */
    public void evictPermissionDefinitionWithUserImpact(UUID permissionId) {
        evictPermissionDefinition(permissionId);
        clear(USER_PERMISSIONS);
        clear(USER_PERMISSION_CODES);
        clear(USER_DETAILS);
        clear(USER_MENU_TREE);
        clear(USER_INFO);
        clear(USER);
    }

    /**
     * 按 username 精确失效 userDetails。供“用户名被修改”时同时失效新 username 使用。
     */
    public void evictUserDetails(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        Cache cache = cacheManager.getCache(USER_DETAILS);
        if (cache != null) {
            cache.evict(username);
        }
    }

    private void evictBare(String cacheName, UUID userId) {
        if (userId == null) {
            return;
        }
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(userId);
        }
    }

    /** {@code userDataScope} 的数值码值键（{@code UserCrossDatabaseQueryService} 负责写入）。 */
    private void evictScopeCode(String cacheName, UUID userId) {
        if (userId == null) {
            return;
        }
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict("scope:" + userId);
        }
    }

    private void evictTenantKeyed(String cacheName, UUID userId) {
        if (userId == null) {
            return;
        }
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return;
        }
        UUID tenantId = TenantContextHolder.getTenantId();
        String key = (tenantId != null ? tenantId.toString() : "no-tenant") + ":" + userId;
        cache.evict(key);
    }

    private void evictKey(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
