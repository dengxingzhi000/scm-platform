package com.cognora.platform.tenant.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.tenant.domain.entity.TenantConfig;
import com.cognora.platform.tenant.mapper.TenantConfigMapper;
import com.cognora.platform.tenant.support.PlatformAdminGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * tenant_config 已从 MyBatis 租户拦截器排除（需要表达 tenant_id IS NULL），
 * 因此租户归属必须在服务层显式校验。
 */
@ExtendWith(MockitoExtension.class)
class TenantConfigServiceImplTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");
    private static final String OTHER_TENANT = "019a0aee-0000-7000-8000-000000000099";

    @Mock
    private TenantConfigMapper mapper;

    @Mock
    private PlatformAdminGuard platformAdminGuard;

    private TenantConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TenantConfigServiceImpl(platformAdminGuard);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void platformScopeShouldBeForbiddenForTenantAdmin() {
        when(platformAdminGuard.isPlatformAdmin()).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.resolveTenantFilter("PLATFORM", null));
        assertThrows(AccessDeniedException.class,
                () -> service.pageQuery(1, 10, null, null, null, "PLATFORM"));
        verify(mapper, never()).selectPage(any(), any());
    }

    @Test
    void platformScopeShouldReturnPlatformRowsForPlatformAdmin() {
        when(platformAdminGuard.isPlatformAdmin()).thenReturn(true);
        when(mapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        Page<TenantConfig> result = service.pageQuery(1, 10, null, null, null, "PLATFORM");

        assertNotNull(result);
        assertNull(service.resolveTenantFilter("PLATFORM", null));
    }

    @Test
    void tenantScopeShouldAlwaysUseTheCurrentTenant() {
        assertEquals(TENANT.toString(), service.resolveTenantFilter(null, OTHER_TENANT));
        assertEquals(TENANT.toString(), service.resolveTenantFilter("TENANT", OTHER_TENANT));
    }

    @Test
    void unknownScopeShouldBeRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.resolveTenantFilter("GLOBAL", null));
    }

    @Test
    void createConfigShouldDefaultToTheCurrentTenant() {
        TenantConfig entity = config(null);

        service.createConfig(entity);

        assertEquals(TENANT.toString(), entity.getTenantId());
        verify(mapper).insert(any(TenantConfig.class));
    }

    @Test
    void createConfigShouldRejectAForeignTenantForTenantAdmin() {
        when(platformAdminGuard.isPlatformAdmin()).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.createConfig(config(OTHER_TENANT)));
        verify(mapper, never()).insert(any(TenantConfig.class));
    }

    @Test
    void getByIdShouldRejectAForeignTenantConfig() {
        when(platformAdminGuard.isPlatformAdmin()).thenReturn(false);
        when(mapper.selectOne(any())).thenReturn(config(OTHER_TENANT));

        assertThrows(AccessDeniedException.class, () -> service.getById("cfg-1"));
    }

    @Test
    void getByIdShouldRejectAPlatformConfigForTenantAdmin() {
        when(platformAdminGuard.isPlatformAdmin()).thenReturn(false);
        when(mapper.selectOne(any())).thenReturn(config(null));

        assertThrows(AccessDeniedException.class, () -> service.getById("cfg-1"));
    }

    @Test
    void deleteByIdShouldRejectAForeignTenantConfig() {
        when(platformAdminGuard.isPlatformAdmin()).thenReturn(false);
        when(mapper.selectOne(any())).thenReturn(config(OTHER_TENANT));

        assertThrows(AccessDeniedException.class, () -> service.deleteById("cfg-1"));
        verify(mapper, never()).deleteById(anyString());
    }

    @Test
    void getConfigByTenantAndKeyShouldRejectAForeignTenant() {
        when(platformAdminGuard.isPlatformAdmin()).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.getConfigByTenantAndKey(OTHER_TENANT, "theme"));
        verify(mapper, never()).selectOne(any());
    }

    private TenantConfig config(String tenantId) {
        TenantConfig config = new TenantConfig();
        config.setTenantId(tenantId);
        config.setConfigCategory("SYSTEM");
        config.setConfigKey("theme");
        return config;
    }
}
