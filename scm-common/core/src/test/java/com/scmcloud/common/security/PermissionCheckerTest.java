package com.scmcloud.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionCheckerTest {

    @Mock
    PermissionQueryService query;

    PermissionChecker checker;

    @BeforeEach
    void setup() {
        checker = new PermissionChecker(query, new ObjectMapper());
    }

    @Test
    void hasPermission_returnsTrueWhenPresent() {
        UUID userId = UUID.randomUUID();
        when(query.getUserPermissions(userId)).thenReturn(Set.of("order:create"));
        assertThat(checker.hasPermission(userId, "order:create")).isTrue();
    }

    @Test
    void hasPermission_returnsFalseWhenMissing() {
        UUID userId = UUID.randomUUID();
        when(query.getUserPermissions(userId)).thenReturn(Set.of("order:create"));
        assertThat(checker.hasPermission(userId, "order:delete")).isFalse();
    }

    @Test
    void hasPermission_returnsFalseOnNullArgs() {
        UUID userId = UUID.randomUUID();
        assertThat(checker.hasPermission(null, "order:create")).isFalse();
        assertThat(checker.hasPermission(userId, null)).isFalse();
        assertThat(checker.hasPermission(userId, "")).isFalse();
    }

    @Test
    void hasPermission_cachesAcrossCalls() {
        UUID userId = UUID.randomUUID();
        when(query.getUserPermissions(userId)).thenReturn(Set.of("order:create"));
        checker.hasPermission(userId, "order:create");
        checker.hasPermission(userId, "order:create");
        checker.hasPermission(userId, "order:create");
        verify(query, times(1)).getUserPermissions(userId);
    }

    @Test
    void hasRole_returnsTrueWhenPresent() {
        UUID userId = UUID.randomUUID();
        when(query.getUserRoles(userId)).thenReturn(Set.of("ADMIN"));
        assertThat(checker.hasRole(userId, "ADMIN")).isTrue();
    }

    @Test
    void requirePermission_throwsWhenMissing() {
        UUID userId = UUID.randomUUID();
        when(query.getUserPermissions(userId)).thenReturn(Set.of("order:create"));
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> checker.requirePermission(userId, "order:delete"))
                .isInstanceOf(com.scmcloud.common.exception.BusinessException.class);
    }
}
