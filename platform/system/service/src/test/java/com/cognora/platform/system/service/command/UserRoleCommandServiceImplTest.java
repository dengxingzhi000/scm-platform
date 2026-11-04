package com.cognora.platform.system.service.command;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.system.cache.UserAuthorizationCacheEvictor;
import com.cognora.platform.system.domain.entity.SysUser;
import com.cognora.platform.system.mapper.SysUserMapper;
import com.cognora.platform.system.service.query.UserCrossDatabaseQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRoleCommandServiceImplTest {

    private static final List<String> CACHE_NAMES = List.of(
            "user", "userInfo", "userDetails",
            "userRoles", "userPermissions", "userRoleCodes", "userPermissionCodes",
            "userRolesWithNames", "userDataScope", "userMaxRoleLevel", "userTemporaryRoles",
            "userMenuTree", "userDeptId", "accessibleDeptIds");

    @Mock private SysUserMapper userMapper;
    @Mock private UserCrossDatabaseQueryService userQueryService;
    @Mock private UserRoleCrossDatabaseCommandService userRoleCommandService;
    @Mock private CacheManager cacheManager;

    private final Map<String, Cache> caches = new HashMap<>();
    private UserRoleCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        caches.clear();
        for (String name : CACHE_NAMES) {
            caches.put(name, mock(Cache.class));
        }
        lenient().when(cacheManager.getCache(anyString()))
                .thenAnswer(invocation -> caches.get(invocation.getArgument(0)));

        UserAuthorizationCacheEvictor evictor = new UserAuthorizationCacheEvictor(cacheManager);
        service = new UserRoleCommandServiceImpl(userMapper, userQueryService,
                userRoleCommandService, evictor);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void grantTemporaryRolesEvictsAuthorizationCachesWithExactKeys() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername("tester");
        when(userMapper.selectById(userId)).thenReturn(user);

        service.grantTemporaryRoles(userId, List.of(UUID.randomUUID()),
                null, LocalDateTime.now().plusDays(1));

        assertAuthorizationEvictions(tenantId, userId, "tester");
    }

    @Test
    void extendTemporaryRoleEvictsAuthorizationCachesWithExactKeys() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername("tester");
        when(userQueryService.hasTemporaryRole(userId, roleId)).thenReturn(true);
        when(userRoleCommandService.extendTemporaryRole(eq(userId), eq(roleId), any(LocalDateTime.class)))
                .thenReturn(1);
        when(userMapper.selectById(userId)).thenReturn(user);

        service.extendTemporaryRole(userId, roleId, LocalDateTime.now().plusDays(2));

        assertAuthorizationEvictions(tenantId, userId, "tester");
    }

    @Test
    void terminateTemporaryRoleEvictsAuthorizationCachesWithExactKeys() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername("tester");
        when(userRoleCommandService.terminateTemporaryRole(userId, roleId)).thenReturn(1);
        when(userMapper.selectById(userId)).thenReturn(user);

        service.terminateTemporaryRole(userId, roleId);

        assertAuthorizationEvictions(tenantId, userId, "tester");
    }

    private void assertAuthorizationEvictions(UUID tenantId, UUID userId, String username) {
        verify(cache("user")).evict(tenantId + ":" + userId);
        verify(cache("userInfo")).evict(tenantId + ":" + userId);
        verify(cache("userDetails")).evict(tenantId + ":" + username);
        verify(cache("userRoles")).evict(userId);
        verify(cache("userRoleCodes")).evict(userId);
        verify(cache("userPermissionCodes")).evict(userId);
        verify(cache("userRolesWithNames")).evict(userId);
        verify(cache("userMaxRoleLevel")).evict(userId);
        verify(cache("userMenuTree")).evict(userId);
        verify(cache("userDataScope")).evict(userId);
        verify(cache("userDataScope")).evict("scope:" + userId);
        verify(cache("userDataScope")).evict(tenantId + ":" + userId);
        verify(cache("userTemporaryRoles")).evict(userId);
        verify(cache("userTemporaryRoles")).evict(tenantId + ":" + userId);
        verify(cache("userPermissions")).clear();
        verify(cache("accessibleDeptIds")).clear();
    }

    private Cache cache(String name) {
        return caches.get(name);
    }
}
