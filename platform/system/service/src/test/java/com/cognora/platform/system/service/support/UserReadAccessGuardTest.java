package com.cognora.platform.system.service.support;

import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.security.PermissionChecker;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.web.domain.SecurityUser;
import com.cognora.platform.system.domain.entity.SysUser;
import com.cognora.platform.system.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserReadAccessGuardTest {

    private SysUserMapper userMapper;
    private PermissionChecker permissionChecker;
    private UserReadAccessGuard guard;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        permissionChecker = mock(PermissionChecker.class);
        guard = new UserReadAccessGuard(userMapper, permissionChecker);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsCrossTenantUser() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        authenticate(userId);

        SysUser target = new SysUser().setId(UUID.randomUUID()).setTenantId(UUID.randomUUID());
        when(userMapper.selectById(target.getId())).thenReturn(target);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.requireReadable(target.getId()));
        assertEquals(1303, ex.getCode());
    }

    @Test
    void allowsAllScope() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        authenticate(userId);

        SysUser target = new SysUser().setId(UUID.randomUUID()).setTenantId(tenantId);
        when(userMapper.selectById(target.getId())).thenReturn(target);
        when(permissionChecker.getUserDataScope(any())).thenReturn("ALL");

        assertEquals(target, guard.requireReadable(target.getId()));
    }

    @Test
    void rejectsSelfScopeForOtherOwner() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        authenticate(userId);

        SysUser target = new SysUser().setId(UUID.randomUUID()).setTenantId(tenantId)
                .setCreateBy(UUID.randomUUID());
        when(userMapper.selectById(target.getId())).thenReturn(target);
        when(permissionChecker.getUserDataScope(any())).thenReturn("SELF");
        when(permissionChecker.cannotOperateResource(any(), any(), any(), anyString())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.requireReadable(target.getId()));
        assertEquals(1205, ex.getCode());
    }

    private void authenticate(UUID userId) {
        SecurityUser principal = SecurityUser.builder().userId(userId).username("tester").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
