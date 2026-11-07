package com.cognora.platform.system.service.support;

import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.system.config.PasswordPolicyProperties;
import com.cognora.platform.system.domain.entity.SysUserPasswordHistory;
import com.cognora.platform.system.mapper.SysUserPasswordHistoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordPolicyServiceTest {

    private PasswordPolicyProperties properties;
    private SysUserPasswordHistoryMapper historyMapper;
    private PasswordEncoder passwordEncoder;
    private PasswordPolicyService service;

    @BeforeEach
    void setUp() {
        properties = new PasswordPolicyProperties();
        historyMapper = mock(SysUserPasswordHistoryMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new PasswordPolicyService(properties, historyMapper, passwordEncoder);
    }

    @Test
    void rejectsPasswordWithoutUppercase() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validate("abcd1234!"));
        assertEquals(1012, ex.getCode());
    }

    @Test
    void rejectsPasswordLongerThanBcryptLimit() {
        String longPassword = "A1!" + "a".repeat(80);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validate(longPassword));
        assertEquals(1012, ex.getCode());
    }

    @Test
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> service.validate("Abcd1234!"));
    }

    @Test
    void rejectsPasswordReusedInHistory() {
        when(historyMapper.selectRecentHashes(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of("old-hash"));
        when(passwordEncoder.matches("Abcd1234!", "old-hash")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateNotReused(java.util.UUID.randomUUID(), "Abcd1234!"));
        assertEquals(1013, ex.getCode());
    }

    @Test
    void recordsHistoryEntry() {
        assertDoesNotThrow(() -> service.record(java.util.UUID.randomUUID(), "encoded", "CHANGE"));
        verify(historyMapper).insert(org.mockito.ArgumentMatchers.any(SysUserPasswordHistory.class));
        verify(passwordEncoder, never()).encode(anyString());
    }
}
