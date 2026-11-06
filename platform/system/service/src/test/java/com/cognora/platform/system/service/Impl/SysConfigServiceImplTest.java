package com.cognora.platform.system.service.Impl;

import com.cognora.platform.common.dto.config.SysConfigItemDTO;
import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.system.domain.entity.SysConfig;
import com.cognora.platform.system.mapper.SysConfigMapper;
import com.cognora.platform.system.service.support.PlatformAdminGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 配置读取按租户覆盖优先；写平台级仅平台超管可执行。
 */
@ExtendWith(MockitoExtension.class)
class SysConfigServiceImplTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    @Mock private SysConfigMapper configMapper;
    @Mock private PlatformAdminGuard platformAdminGuard;
    @InjectMocks private SysConfigServiceImpl service;

    @BeforeEach
    void bindTenant() {
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void mergedViewMarksPlatformRowsAndOverridesThemWithTenantRows() {
        SysConfig platformA = row("order.autoCancelMinutes", "30", null);
        SysConfig platformB = row("order.maxLines", "50", null);
        SysConfig tenantA = row("order.autoCancelMinutes", "15", TENANT);
        when(configMapper.selectList(any())).thenReturn(List.of(platformA, platformB), List.of(tenantA));

        List<SysConfigItemDTO> merged = service.getMergedConfig();

        assertEquals(2, merged.size());
        assertEquals("order.autoCancelMinutes", merged.get(0).getConfigKey());
        assertEquals("15", merged.get(0).getConfigValue());
        assertEquals("TENANT", merged.get(0).getScope());
        assertEquals(TENANT, merged.get(0).getTenantId());
        assertEquals("order.maxLines", merged.get(1).getConfigKey());
        assertEquals("50", merged.get(1).getConfigValue());
        assertEquals("PLATFORM", merged.get(1).getScope());
        assertNull(merged.get(1).getTenantId());
    }

    @Test
    void tenantAdminCannotWritePlatformScope() {
        when(platformAdminGuard.isPlatformAdmin()).thenReturn(false);
        SysConfigItemDTO request = item("order.maxLines", "80", "PLATFORM");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateConfig(request));

        assertEquals(403, exception.getCode());
        verifyNoInteractions(configMapper);
    }

    @Test
    void tenantScopeWritesCurrentTenantOverrideWithoutPlatformCheck() {
        SysConfig saved = row("order.maxLines", "80", TENANT);
        when(configMapper.selectOne(any())).thenReturn(null, saved);

        SysConfigItemDTO response = service.updateConfig(item("order.maxLines", "80", null));

        ArgumentCaptor<SysConfig> captor = ArgumentCaptor.forClass(SysConfig.class);
        verify(configMapper).insert(captor.capture());
        assertEquals(TENANT, captor.getValue().getTenantId());
        assertEquals("TENANT", response.getScope());
        assertEquals(TENANT, response.getTenantId());
        assertEquals("80", response.getConfigValue());
        verifyNoInteractions(platformAdminGuard);
    }

    @Test
    void platformAdminWritesPlatformRowWithNullTenant() {
        when(platformAdminGuard.isPlatformAdmin()).thenReturn(true);
        SysConfig saved = row("order.maxLines", "80", null);
        when(configMapper.selectOne(any())).thenReturn(null, saved);

        SysConfigItemDTO response = service.updateConfig(item("order.maxLines", "80", "PLATFORM"));

        ArgumentCaptor<SysConfig> captor = ArgumentCaptor.forClass(SysConfig.class);
        verify(configMapper).insert(captor.capture());
        assertNull(captor.getValue().getTenantId());
        assertEquals("PLATFORM", response.getScope());
        assertNull(response.getTenantId());
    }

    @Test
    void existingRowIsUpdatedInsteadOfDuplicated() {
        SysConfig existing = row("order.maxLines", "50", TENANT);
        when(configMapper.selectOne(any())).thenReturn(existing);

        SysConfigItemDTO response = service.updateConfig(item("order.maxLines", "99", "TENANT"));

        assertEquals("99", existing.getConfigValue());
        verify(configMapper).updateById(existing);
        verify(configMapper, never()).insert(any(SysConfig.class));
        assertEquals("99", response.getConfigValue());
    }

    private static SysConfig row(String key, String value, UUID tenantId) {
        return new SysConfig()
                .setId(UUID.randomUUID())
                .setTenantId(tenantId)
                .setConfigKey(key)
                .setConfigValue(value)
                .setValueType("STRING")
                .setUpdateTime(LocalDateTime.now());
    }

    private static SysConfigItemDTO item(String key, String value, String scope) {
        SysConfigItemDTO item = new SysConfigItemDTO();
        item.setConfigKey(key);
        item.setConfigValue(value);
        item.setValueType("STRING");
        item.setScope(scope);
        return item;
    }
}
