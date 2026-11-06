package com.cognora.scm.finance.service.support;

import com.cognora.platform.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinanceTenantGuardTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void sameTenantShouldPass() {
        assertDoesNotThrow(() -> FinanceTenantGuard.verifyOwnership(TENANT.toString()));
    }

    @Test
    void foreignTenantShouldBeRejectedWith403() {
        assertThrows(AccessDeniedException.class,
                () -> FinanceTenantGuard.verifyOwnership("019a0aee-0000-7000-8000-000000000099"));
    }

    @Test
    void blankTenantShouldPassForLegacyRows() {
        assertDoesNotThrow(() -> FinanceTenantGuard.verifyOwnership(null));
        assertDoesNotThrow(() -> FinanceTenantGuard.verifyOwnership(" "));
    }
}
