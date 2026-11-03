package com.scmcloud.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextHolderTest {

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void setAndGet_returnsSameTenant() {
        UUID id = UUID.randomUUID();
        TenantContextHolder.setTenantId(id);
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(id);
    }

    @Test
    void clear_removesTenant() {
        TenantContextHolder.setTenantId(UUID.randomUUID());
        TenantContextHolder.clear();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void getRequiredTenantId_throwsWhenNull() {
        assertThatThrownBy(() -> TenantContextHolder.getRequiredTenantId())
                .isInstanceOf(TenantParseException.class);
    }

    @Test
    void getRequiredTenantId_returnsWhenSet() {
        UUID id = UUID.randomUUID();
        TenantContextHolder.setTenantId(id);
        assertThat(TenantContextHolder.getRequiredTenantId()).isEqualTo(id);
    }

    @Test
    void executeInTenantContext_restoresAfterCallback() {
        UUID outer = UUID.randomUUID();
        UUID inner = UUID.randomUUID();
        TenantContextHolder.setTenantId(outer);
        UUID observed = TenantContextHolder.executeInTenantContext(inner,
                () -> TenantContextHolder.getTenantId());
        assertThat(observed).isEqualTo(inner);
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(outer);
    }

    @Test
    void executeInTenantContext_clearsWhenNoOriginal() {
        UUID inner = UUID.randomUUID();
        UUID observed = TenantContextHolder.executeInTenantContext(inner,
                () -> TenantContextHolder.getTenantId());
        assertThat(observed).isEqualTo(inner);
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }
}
