package com.cognora.lms.logistics.support;

import com.cognora.platform.common.security.PermissionChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 批次 3：对账确认权限守卫用例。
 * 权限组件启用时按权限码校验；未启用时保持部署可用性。
 */
class FreightPermissionGuardTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void guardShouldCheckPermissionCodeWhenCheckerAvailable() {
        PermissionChecker checker = mock(PermissionChecker.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<PermissionChecker> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(checker);
        FreightPermissionGuard guard = new FreightPermissionGuard(provider);

        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));

        guard.requireReconciliationConfirmPermission();

        verify(checker).requirePermission(userId, FreightPermissionGuard.RECONCILIATION_CONFIRM);
    }

    @Test
    void guardShouldNoOpWhenPermissionCheckerDisabled() {
        @SuppressWarnings("unchecked")
        ObjectProvider<PermissionChecker> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        FreightPermissionGuard guard = new FreightPermissionGuard(provider);

        assertDoesNotThrow(guard::requireReconciliationConfirmPermission);
        verify(provider).getIfAvailable();
        verify(provider, never()).getObject();
    }
}
