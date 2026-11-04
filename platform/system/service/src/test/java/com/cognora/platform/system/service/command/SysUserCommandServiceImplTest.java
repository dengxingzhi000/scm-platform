package com.cognora.platform.system.service.command;

import com.cognora.platform.common.dto.user.UserDTO;
import com.cognora.platform.common.rest.client.SysAuthServiceClient;
import com.cognora.platform.common.security.PermissionChecker;
import com.cognora.platform.common.status.StatusValidator;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.web.domain.SecurityUser;
import com.cognora.platform.system.cache.UserAuthorizationCacheEvictor;
import com.cognora.platform.system.domain.entity.SysUser;
import com.cognora.platform.system.event.DataSyncEventPublisher;
import com.cognora.platform.system.mapper.SysUserMapper;
import com.cognora.platform.system.service.query.RoleCrossDatabaseQueryService;
import com.cognora.platform.system.service.query.UserCrossDatabaseQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysUserCommandServiceImplTest {

    private static final List<String> CACHE_NAMES = List.of(
            "user", "userInfo", "userDetails",
            "userRoles", "userPermissions", "userRoleCodes", "userPermissionCodes",
            "userRolesWithNames", "userDataScope", "userMaxRoleLevel", "userTemporaryRoles",
            "userMenuTree", "userDeptId", "accessibleDeptIds",
            "role", "roles", "rolePermissions", "roleLevel",
            "permission", "permissionTree", "apiPermissions");

    @Mock private SysUserMapper userMapper;
    @Mock private UserCrossDatabaseQueryService userQueryService;
    @Mock private RoleCrossDatabaseQueryService roleQueryService;
    @Mock private UserRoleCrossDatabaseCommandService userRoleCommandService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private DataSyncEventPublisher dataSyncEventPublisher;
    @Mock private PermissionChecker permissionChecker;
    @Mock private StatusValidator statusValidator;
    @Mock private UserCommandTemplate commandTemplate;
    @Mock private SysAuthServiceClient authServiceClient;
    @Mock private CacheManager cacheManager;

    private final Map<String, Cache> caches = new HashMap<>();
    private SysUserCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        caches.clear();
        for (String name : CACHE_NAMES) {
            caches.put(name, mock(Cache.class));
        }
        lenient().when(cacheManager.getCache(anyString()))
                .thenAnswer(invocation -> caches.get(invocation.getArgument(0)));

        UserAuthorizationCacheEvictor evictor = new UserAuthorizationCacheEvictor(cacheManager);
        service = new SysUserCommandServiceImpl(userMapper, userQueryService, roleQueryService,
                userRoleCommandService, passwordEncoder, dataSyncEventPublisher, permissionChecker,
                statusValidator, commandTemplate, authServiceClient, evictor);
    }

    @AfterEach
    void clearContexts() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void forceLogoutDelegatesToAuthServiceClient() {
        UUID userId = UUID.randomUUID();

        service.forceLogout(userId, "security policy");

        verify(authServiceClient).forceLogout(userId, "security policy");
    }

    @Test
    void updateUserBindsPathVariableIdToDto() {
        UUID userId = UUID.randomUUID();
        UserDTO userDTO = new UserDTO();
        when(commandTemplate.execute(eq("UPDATE"), eq("user:update"), eq(userId), any()))
                .thenReturn(new SysUser());

        service.updateUser(userId, userDTO);

        assertEquals(userId, userDTO.getId());
        verify(commandTemplate).execute(eq("UPDATE"), eq("user:update"), eq(userId), any());
    }

    @Test
    void changePasswordEvictsUserByTenantKeyAndUserDetailsByUsername() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        authenticate(userId, "tester");

        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername("tester");
        user.setPassword("encoded-old");

        when(userMapper.selectById(userId)).thenReturn(user);
        when(passwordEncoder.matches("old-pass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.matches("new-pass", "encoded-old")).thenReturn(false);
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new");
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        service.changePassword("old-pass", "new-pass");

        // 密码先落库，再失效缓存；断言的是实际传入 evict 的 key 内容。
        InOrder order = inOrder(userMapper, cache("user"), cache("userDetails"));
        order.verify(userMapper).updateById(any(SysUser.class));
        order.verify(cache("user")).evict(tenantId + ":" + userId);
        order.verify(cache("userDetails")).evict(tenantId + ":tester");
    }

    @Test
    void updateUserEvictsUserInfoAndUserDetailsByExactKeys() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        SysUser loaded = new SysUser();
        loaded.setId(userId);
        loaded.setUsername("tester");

        when(commandTemplate.execute(eq("UPDATE"), eq("user:update"), eq(userId), any()))
                .thenReturn(loaded);
        when(userMapper.selectById(userId)).thenReturn(loaded);

        service.updateUser(userId, new UserDTO());

        verify(cache("user")).evict(tenantId + ":" + userId);
        verify(cache("userInfo")).evict(tenantId + ":" + userId);
        verify(cache("userDetails")).evict(tenantId + ":tester");
    }

    @Test
    void resetPasswordEvictsUserByTenantKeyAndUserDetailsByUsername() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername("tester");
        user.setTenantId(tenantId);

        when(userMapper.selectById(userId)).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-new");
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        service.resetPassword(userId);

        verify(cache("user")).evict(tenantId + ":" + userId);
        verify(cache("userDetails")).evict(tenantId + ":tester");
    }

    @Test
    void grantRolesEvictsAuthorizationCachesWithExactKeys() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername("tester");
        user.setTenantId(tenantId);
        when(userMapper.selectById(userId)).thenReturn(user);

        service.grantRoles(userId, List.of(UUID.randomUUID()));

        assertAuthorizationEvictions(tenantId, userId, "tester");
    }

    @Test
    void revokeRolesEvictsAuthorizationCachesWithExactKeys() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername("tester");
        user.setTenantId(tenantId);
        when(userMapper.selectById(userId)).thenReturn(user);

        service.grantRoles(userId, List.of());

        assertAuthorizationEvictions(tenantId, userId, "tester");
    }

    @Test
    void updateUserDeptChangeEvictsUserDeptAndAccessibleDeptCaches() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID oldDeptId = UUID.randomUUID();
        UUID newDeptId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        SysUser loaded = new SysUser();
        loaded.setId(userId);
        loaded.setUsername("tester");
        loaded.setDeptId(oldDeptId);

        UserDTO userDTO = new UserDTO();
        userDTO.setDeptId(newDeptId);

        when(commandTemplate.execute(eq("UPDATE"), eq("user:update"), eq(userId), any()))
                .thenReturn(loaded);
        when(userMapper.selectById(userId)).thenReturn(loaded);

        service.updateUser(userId, userDTO);

        verify(cache("user")).evict(tenantId + ":" + userId);
        verify(cache("userInfo")).evict(tenantId + ":" + userId);
        verify(cache("userDetails")).evict(tenantId + ":tester");
        verify(cache("userDeptId")).evict(userId);
        verify(cache("accessibleDeptIds")).clear();
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
        // userDataScope 的三种键格式都必须失效：String 枚举、Integer 码值、KEY_GEN。
        verify(cache("userDataScope")).evict(userId);
        verify(cache("userDataScope")).evict("scope:" + userId);
        verify(cache("userDataScope")).evict(tenantId + ":" + userId);
        // userTemporaryRoles 的两套键格式都必须失效。
        verify(cache("userTemporaryRoles")).evict(userId);
        verify(cache("userTemporaryRoles")).evict(tenantId + ":" + userId);
        // 复合键只能整体清理。
        verify(cache("userPermissions")).clear();
        verify(cache("accessibleDeptIds")).clear();
    }

    private Cache cache(String name) {
        return caches.get(name);
    }

    private static void authenticate(UUID userId, String username) {
        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .username(username)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
