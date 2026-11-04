package com.cognora.ecom.search.tenant;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.web.domain.SecurityUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchTenantResolverTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private final SearchTenantResolver resolver = new SearchTenantResolver();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void tokenTenantWinsWhenContextMatches() {
        authenticate(TENANT_A);
        TenantContextHolder.setTenantId(TENANT_A);

        assertThat(resolver.resolveTenantId()).isEqualTo(TENANT_A);
    }

    @Test
    void rejectsWhenHeaderTenantDiffersFromTokenTenant() {
        authenticate(TENANT_A);
        TenantContextHolder.setTenantId(TENANT_B);

        assertThatThrownBy(resolver::resolveTenantId)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void anonymousRequestUsesTenantContext() {
        TenantContextHolder.setTenantId(TENANT_A);

        assertThat(resolver.resolveTenantId()).isEqualTo(TENANT_A);
    }

    @Test
    void rejectsWhenNoTenantIsAvailable() {
        assertThatThrownBy(resolver::resolveTenantId)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("required");
    }

    static void authenticate(UUID tenantId) {
        SecurityUser principal = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .username("tester")
                .tenantId(tenantId)
                .roles(Set.of())
                .permissions(Set.of())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
