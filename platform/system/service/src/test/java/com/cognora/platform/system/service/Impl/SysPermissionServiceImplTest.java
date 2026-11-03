package com.cognora.platform.system.service.Impl;

import com.cognora.platform.common.dto.permission.PermissionDTO;
import com.cognora.platform.common.security.PermissionChecker;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.system.cache.UserAuthorizationCacheEvictor;
import com.cognora.platform.system.domain.entity.SysPermission;
import com.cognora.platform.system.mapper.SysPermissionMapper;
import com.cognora.platform.system.mapper.SysRolePermissionMapper;
import com.cognora.platform.system.mapper.SysTempPermissionMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysPermissionServiceImplTest {

    private static final List<String> CACHE_NAMES = List.of(
            "user", "userInfo", "userDetails",
            "userPermissions", "userPermissionCodes", "userMenuTree",
            "permission", "permissionTree", "apiPermissions");

    @Mock private SysPermissionMapper permissionMapper;
    @Mock private SysTempPermissionMapper tempPermissionMapper;
    @Mock private SysRolePermissionMapper rolePermissionMapper;
    @Mock private PermissionChecker permissionChecker;
    @Mock private CacheManager cacheManager;

    private final Map<String, Cache> caches = new HashMap<>();
    private SysPermissionServiceImpl service;

    @BeforeEach
    void setUp() {
        caches.clear();
        for (String name : CACHE_NAMES) {
            caches.put(name, mock(Cache.class));
        }
        lenient().when(cacheManager.getCache(anyString()))
                .thenAnswer(invocation -> caches.get(invocation.getArgument(0)));

        UserAuthorizationCacheEvictor evictor = new UserAuthorizationCacheEvictor(cacheManager);
        service = new SysPermissionServiceImpl(tempPermissionMapper, rolePermissionMapper,
                permissionChecker, evictor);
        ReflectionTestUtils.setField(service, "baseMapper", permissionMapper);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void updatePermissionEvictsDefinitionAndUserPermissionCachesWithExactKeys() {
        UUID tenantId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        SysPermission existing = new SysPermission();
        existing.setId(permissionId);
        existing.setTenantId(tenantId);
        existing.setPermissionScope("TENANT");
        when(permissionMapper.selectById(permissionId)).thenReturn(existing);
        when(permissionMapper.updateById(any(SysPermission.class))).thenReturn(1);

        PermissionDTO dto = new PermissionDTO();
        dto.setPermissionCode("system:user:list");

        service.updatePermission(permissionId, dto);

        verify(cache("permission")).evict(permissionId);
        verify(cache("permissionTree")).clear();
        verify(cache("apiPermissions")).clear();
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
