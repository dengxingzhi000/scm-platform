package com.scmcloud.system.cache;

import com.scmcloud.common.cache.TenantAwareCacheKeyGenerator;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.system.service.query.SysUserQueryServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAuthorizationCacheEvictorTest {

    private static final List<String> CACHE_NAMES = List.of(
            "user", "userInfo", "userDetails",
            "userRoles", "userPermissions", "userRoleCodes", "userPermissionCodes",
            "userRolesWithNames", "userDataScope", "userMaxRoleLevel", "userTemporaryRoles",
            "userMenuTree", "userDeptId", "accessibleDeptIds",
            "role", "roles", "rolePermissions", "roleLevel",
            "permission", "permissionTree", "apiPermissions");

    @Mock private CacheManager cacheManager;

    private final Map<String, Cache> caches = new HashMap<>();
    private UserAuthorizationCacheEvictor evictor;

    @BeforeEach
    void setUp() {
        caches.clear();
        for (String name : CACHE_NAMES) {
            caches.put(name, mock(Cache.class));
        }
        lenient().when(cacheManager.getCache(anyString()))
                .thenAnswer(invocation -> caches.get(invocation.getArgument(0)));
        evictor = new UserAuthorizationCacheEvictor(cacheManager);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void evictAllUserAuthorizationClearsEveryUserAuthorizationCache() {
        evictor.evictAllUserAuthorization();

        verify(cache("user")).clear();
        verify(cache("userInfo")).clear();
        verify(cache("userDetails")).clear();
        verify(cache("userRoles")).clear();
        verify(cache("userPermissions")).clear();
        verify(cache("userRoleCodes")).clear();
        verify(cache("userPermissionCodes")).clear();
        verify(cache("userRolesWithNames")).clear();
        verify(cache("userDataScope")).clear();
        verify(cache("userMaxRoleLevel")).clear();
        verify(cache("userTemporaryRoles")).clear();
        verify(cache("userMenuTree")).clear();
        verify(cache("accessibleDeptIds")).clear();
    }

    @Test
    void evictUserAuthorizationEvictsAllThreeUserDataScopeKeyShapes() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        evictor.evictUserAuthorization(userId, "tester");

        // String 枚举（PermissionQueryServiceImpl）、Integer 码值（UserCrossDatabaseQueryService）、
        // KEY_GEN（SysUserQueryServiceImpl）三种键由不同实现写入，必须全部失效。
        verify(cache("userDataScope")).evict(userId);
        verify(cache("userDataScope")).evict("scope:" + userId);
        verify(cache("userDataScope")).evict(tenantId + ":" + userId);
    }

    @Test
    void tenantAwareKeyGeneratorProducesTheSameKeyTheEvictorRemoves() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        // KEY_GEN 写入路径：SysUserQueryServiceImpl 的单参数方法 → tenantId:userId。
        Method method = SysUserQueryServiceImpl.class.getMethod("getUserDataScope", UUID.class);
        String generatedKey = String.valueOf(
                new TenantAwareCacheKeyGenerator().generate(this, method, userId));
        assertEquals(tenantId + ":" + userId, generatedKey);

        // 失效路径必须删掉同一个字符串键，而不是另一个格式。
        evictor.evictUserAuthorization(userId, "tester");
        verify(cache("userDataScope")).evict(generatedKey);
    }

    @Test
    void evictRoleDefinitionEvictsRoleKeysByUuidAndClearsRoleList() {
        UUID roleId = UUID.randomUUID();

        evictor.evictRoleDefinition(roleId);

        verify(cache("role")).evict(roleId);
        verify(cache("rolePermissions")).evict(roleId);
        verify(cache("roleLevel")).evict(roleId);
        verify(cache("roles")).clear();
    }

    @Test
    void evictRoleListClearsRolesCache() {
        evictor.evictRoleList();

        verify(cache("roles")).clear();
    }

    @Test
    void evictPermissionDefinitionEvictsIdAndAllKeyedEntries() {
        UUID permissionId = UUID.randomUUID();

        evictor.evictPermissionDefinition(permissionId);

        verify(cache("permission")).evict(permissionId);
        verify(cache("permissionTree")).clear();
        verify(cache("apiPermissions")).clear();
    }

    private Cache cache(String name) {
        return caches.get(name);
    }
}
