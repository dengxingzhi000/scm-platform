package com.scmcloud.system.service.Impl;

import com.scmcloud.common.security.PermissionChecker;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.system.cache.UserAuthorizationCacheEvictor;
import com.scmcloud.system.domain.entity.SysRole;
import com.scmcloud.system.event.DataSyncEventPublisher;
import com.scmcloud.system.mapper.SysRoleDataRuleMapper;
import com.scmcloud.system.mapper.SysRoleDeptMapper;
import com.scmcloud.system.mapper.SysRoleMapper;
import com.scmcloud.system.mapper.SysRolePermissionMapper;
import com.scmcloud.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysRoleServiceImplTest {

    private static final List<String> CACHE_NAMES = List.of(
            "user", "userInfo", "userDetails",
            "userRoles", "userPermissions", "userRoleCodes", "userPermissionCodes",
            "userRolesWithNames", "userDataScope", "userMaxRoleLevel", "userTemporaryRoles",
            "userMenuTree", "accessibleDeptIds",
            "role", "roles", "rolePermissions", "roleLevel");

    @Mock private SysRoleMapper roleMapper;
    @Mock private SysUserRoleMapper userRoleMapper;
    @Mock private DataSyncEventPublisher dataSyncEventPublisher;
    @Mock private SysRolePermissionMapper rolePermissionMapper;
    @Mock private SysRoleDeptMapper roleDeptMapper;
    @Mock private SysRoleDataRuleMapper roleDataRuleMapper;
    @Mock private PermissionChecker permissionChecker;
    @Mock private CacheManager cacheManager;

    private final Map<String, Cache> caches = new HashMap<>();
    private SysRoleServiceImpl service;

    @BeforeEach
    void setUp() {
        caches.clear();
        for (String name : CACHE_NAMES) {
            caches.put(name, mock(Cache.class));
        }
        lenient().when(cacheManager.getCache(anyString()))
                .thenAnswer(invocation -> caches.get(invocation.getArgument(0)));

        UserAuthorizationCacheEvictor evictor = new UserAuthorizationCacheEvictor(cacheManager);
        service = new SysRoleServiceImpl(userRoleMapper, dataSyncEventPublisher, rolePermissionMapper,
                roleDeptMapper, roleDataRuleMapper, permissionChecker, evictor);
        ReflectionTestUtils.setField(service, "baseMapper", roleMapper);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void grantPermissionsEvictsRoleAndUserPermissionCachesWithExactKeys() {
        UUID tenantId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        SysRole role = new SysRole();
        role.setId(roleId);
        role.setTenantId(tenantId);
        role.setRoleType("TENANT");
        when(roleMapper.selectById(roleId)).thenReturn(role);

        service.grantPermissions(roleId, List.of(UUID.randomUUID()));

        verify(cache("role")).evict(roleId);
        verify(cache("rolePermissions")).evict(roleId);
        verify(cache("userPermissions")).clear();
        verify(cache("userPermissionCodes")).clear();
        verify(cache("userDetails")).clear();
        verify(cache("userMenuTree")).clear();
        verify(cache("userInfo")).clear();
        verify(cache("user")).clear();
    }

    private Cache cache(String name) {
        return caches.get(name);
    }
}
