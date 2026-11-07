package com.cognora.platform.system.service.support;

import com.cognora.platform.system.cache.UserAuthorizationCacheEvictor;
import com.cognora.platform.system.domain.entity.SysUser;
import com.cognora.platform.system.domain.entity.SysUserRecoveryCode;
import com.cognora.platform.system.mapper.SysUserMapper;
import com.cognora.platform.system.mapper.SysUserRecoveryCodeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TwoFactorPersistenceServiceTest {

    private SysUserMapper userMapper;
    private SysUserRecoveryCodeMapper recoveryCodeMapper;
    private UserAuthorizationCacheEvictor cacheEvictor;
    private TwoFactorPersistenceService service;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        recoveryCodeMapper = mock(SysUserRecoveryCodeMapper.class);
        cacheEvictor = mock(UserAuthorizationCacheEvictor.class);
        service = new TwoFactorPersistenceService(userMapper, recoveryCodeMapper, cacheEvictor);
    }

    @Test
    void enablePersistsSecretAndEvictsCache() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        when(userMapper.selectById(userId))
                .thenReturn(new SysUser().setId(userId).setTenantId(tenantId).setUsername("tester"));

        service.enableTwoFactor(userId, "SECRET");

        verify(userMapper).update(any(), any());
        verify(cacheEvictor).evictUserCredentials(userId, "tester");
    }

    @Test
    void disableClearsSecretAndRecoveryCodes() {
        UUID userId = UUID.randomUUID();
        when(userMapper.selectById(userId))
                .thenReturn(new SysUser().setId(userId).setTenantId(UUID.randomUUID()).setUsername("tester"));

        service.disableTwoFactor(userId);

        verify(recoveryCodeMapper).deleteByUserId(userId);
        verify(cacheEvictor).evictUserCredentials(userId, "tester");
    }

    @Test
    void replaceRecoveryCodesInsertsAllHashes() {
        UUID userId = UUID.randomUUID();
        when(userMapper.selectById(userId))
                .thenReturn(new SysUser().setId(userId).setTenantId(UUID.randomUUID()));

        service.replaceRecoveryCodes(userId, List.of("h1", "h2"));

        verify(recoveryCodeMapper).deleteByUserId(userId);
        verify(recoveryCodeMapper, org.mockito.Mockito.times(2))
                .insert(any(SysUserRecoveryCode.class));
    }

    @Test
    void consumeReturnsTrueWhenRowUpdated() {
        UUID userId = UUID.randomUUID();
        when(recoveryCodeMapper.consume(userId, "hash")).thenReturn(1);

        assertTrue(service.consumeRecoveryCode(userId, "hash"));
    }
}
