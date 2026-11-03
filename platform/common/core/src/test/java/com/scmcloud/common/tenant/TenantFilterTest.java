package com.scmcloud.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TenantFilterTest {

    private TenantFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setup() {
        filter = new TenantFilter(new TenantProperties(true, true, List.of()));
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void teardown() {
        TenantContextHolder.clear();
    }

    @Test
    void headerTenantId_shouldBeSet() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID[] captured = new UUID[1];
        org.mockito.Mockito.doAnswer(inv -> {
            captured[0] = TenantContextHolder.getTenantId();
            return null;
        }).when(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        var req = new MockHttpServletRequest();
        req.addHeader("X-Tenant-Id", tenantId.toString());
        var resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, chain);
        assertThat(captured[0]).isEqualTo(tenantId);
    }

    @Test
    void paramTenantId_shouldBeSet() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID[] captured = new UUID[1];
        org.mockito.Mockito.doAnswer(inv -> {
            captured[0] = TenantContextHolder.getTenantId();
            return null;
        }).when(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        var req = new MockHttpServletRequest();
        req.setParameter("tenantId", tenantId.toString());
        var resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, chain);
        assertThat(captured[0]).isEqualTo(tenantId);
    }

    @Test
    void missingTenant_shouldThrowWhenRequired() {
        var req = new MockHttpServletRequest();
        var resp = new MockHttpServletResponse();
        assertThatThrownBy(() -> filter.doFilter(req, resp, chain))
                .isInstanceOf(TenantParseException.class);
    }

    @Test
    void missingTenant_shouldSkipWhenNotRequired() throws Exception {
        filter = new TenantFilter(new TenantProperties(true, false, List.of()));
        var req = new MockHttpServletRequest();
        var resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, chain);
        assertThat(TenantContextHolder.getTenantId()).isNull();
        verify(chain).doFilter(req, resp);
    }

    @Test
    void invalidTenantIdFormat_shouldThrow() {
        var req = new MockHttpServletRequest();
        req.addHeader("X-Tenant-Id", "not-a-uuid");
        var resp = new MockHttpServletResponse();
        assertThatThrownBy(() -> filter.doFilter(req, resp, chain))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clearAfterRequest_shouldAlwaysRun() throws Exception {
        UUID tenantId = UUID.randomUUID();
        var req = new MockHttpServletRequest();
        req.addHeader("X-Tenant-Id", tenantId.toString());
        var resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, chain);
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void excludePath_shouldSkipFilter() throws Exception {
        filter = new TenantFilter(new TenantProperties(true, true, List.of("/actuator/**")));
        var req = new MockHttpServletRequest("GET", "/actuator/health");
        var resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, chain);
        assertThat(TenantContextHolder.getTenantId()).isNull();
        verify(chain).doFilter(req, resp);
    }
}
