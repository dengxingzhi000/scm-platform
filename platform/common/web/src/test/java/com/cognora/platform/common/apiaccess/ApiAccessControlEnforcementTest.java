package com.cognora.platform.common.apiaccess;

import com.cognora.platform.common.log.service.ISysAuditLogService;
import com.cognora.platform.common.security.PermissionService;
import com.cognora.platform.common.security.config.ApiAccessControlProperties;
import com.cognora.platform.common.security.filter.ApiAccessControlFilter;
import com.cognora.platform.common.security.filter.InternalEndpointSignatureFilter;
import com.cognora.platform.common.security.metrics.SecurityMetrics;
import com.cognora.platform.common.web.domain.SecurityUser;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Default-deny behaviour for URLs missing from the API permission catalog (P0-D).
 *
 * <p>Lives outside {@code com.cognora.platform.common.security} because that package is
 * excluded from test compilation in this module's POM.
 */
class ApiAccessControlEnforcementTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final SecurityMetrics metrics = new SecurityMetrics(meterRegistry);
    private final PermissionService permissionService = Mockito.mock(PermissionService.class);
    private final ApiAccessControlProperties properties = new ApiAccessControlProperties();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deniesUnregisteredEndpointWithExplicitCode() throws Exception {
        properties.setWhitelist(List.of());
        properties.setUnregisteredEndpointPolicy(
                ApiAccessControlProperties.UnregisteredEndpointPolicy.DENY);
        when(permissionService.findPermissionsByUrl(anyString(), anyString())).thenReturn(List.of());
        authenticate();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/unregistered/thing");
        request.addHeader("X-Request-ID", "trace-unregistered");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> proceeded.set(true);

        buildFilter().doFilter(request, response, chain);

        assertThat(proceeded).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("API_NOT_REGISTERED");
        assertThat(response.getContentAsString()).contains("trace-unregistered");
        assertThat(meterRegistry.counter("security.access.unregistered_endpoint_denied").count())
                .isEqualTo(1.0d);
    }

    @Test
    void reportOnlyAllowsUnregisteredEndpointAndCountsIt() throws Exception {
        properties.setWhitelist(List.of());
        properties.setUnregisteredEndpointPolicy(
                ApiAccessControlProperties.UnregisteredEndpointPolicy.REPORT_ONLY);
        when(permissionService.findPermissionsByUrl(anyString(), anyString())).thenReturn(List.of());
        authenticate();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/unregistered/thing");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> proceeded.set(true);

        buildFilter().doFilter(request, response, chain);

        assertThat(proceeded).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(meterRegistry.counter("security.access.unregistered_endpoint_report").count())
                .isEqualTo(1.0d);
    }

    @Test
    void allowsRegisteredEndpointWhenUserHoldsPermission() throws Exception {
        properties.setWhitelist(List.of());
        when(permissionService.findPermissionsByUrl(anyString(), anyString())).thenReturn(List.of("perm:read"));
        when(permissionService.findAllPermissionsByUserId(any(UUID.class))).thenReturn(Set.of("perm:read"));
        authenticate();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/registered/thing");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> proceeded.set(true);

        buildFilter().doFilter(request, response, chain);

        assertThat(proceeded).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void allowsTemplateEndpointResolvedByMatcher() throws Exception {
        properties.setWhitelist(List.of());
        // 模板端点 /api/v1/orders/{id} 由 SysPermissionMapper 的候选预筛 + AntPathMatcher 命中
        when(permissionService.findPermissionsByUrl(anyString(), anyString())).thenReturn(List.of("order:order:view"));
        when(permissionService.findAllPermissionsByUserId(any(UUID.class))).thenReturn(Set.of("order:order:view"));
        authenticate();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders/123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> proceeded.set(true);

        buildFilter().doFilter(request, response, chain);

        assertThat(proceeded).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void allowsExemptPathEvenWhenNotRegistered() throws Exception {
        properties.setWhitelist(List.of());
        properties.setUnregisteredEndpointPolicy(
                ApiAccessControlProperties.UnregisteredEndpointPolicy.DENY);
        when(permissionService.findPermissionsByUrl(anyString(), anyString())).thenReturn(List.of());
        authenticate();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/mall/cart");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> proceeded.set(true);

        buildFilter().doFilter(request, response, chain);

        assertThat(proceeded).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void exemptMatchStillHonoursExplicitExclusions() throws Exception {
        properties.setWhitelist(List.of());
        properties.setUnregisteredEndpointPolicy(
                ApiAccessControlProperties.UnregisteredEndpointPolicy.DENY);
        // /api/v1/payments/* 会命中 /api/v1/payments/page，但分页在排除清单内，必须继续校验权限
        when(permissionService.findPermissionsByUrl(anyString(), anyString())).thenReturn(List.of());
        authenticate();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/payments/page");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> proceeded.set(true);

        buildFilter().doFilter(request, response, chain);

        assertThat(proceeded).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("API_NOT_REGISTERED");
    }

    @Test
    void exemptPathWithMatrixParameterIsStillExempt() throws Exception {
        properties.setWhitelist(List.of());
        properties.setUnregisteredEndpointPolicy(
                ApiAccessControlProperties.UnregisteredEndpointPolicy.DENY);
        when(permissionService.findPermissionsByUrl(anyString(), anyString())).thenReturn(List.of());
        authenticate();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/mall/cart;x=1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> proceeded.set(true);

        buildFilter().doFilter(request, response, chain);

        assertThat(proceeded).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void whitelistedPathNeedsNoAuthentication() throws Exception {
        properties.setWhitelist(List.of("/api/auth/login"));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> proceeded.set(true);

        buildFilter().doFilter(request, response, chain);

        assertThat(proceeded).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void skipsPermissionLookupWhenSignatureAlreadyVerified() throws Exception {
        properties.setWhitelist(List.of());
        properties.setUnregisteredEndpointPolicy(
                ApiAccessControlProperties.UnregisteredEndpointPolicy.DENY);
        when(permissionService.findPermissionsByUrl(anyString(), anyString())).thenReturn(List.of());
        authenticate();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/internal-only/thing");
        request.setAttribute(InternalEndpointSignatureFilter.SIGNATURE_VERIFIED_ATTRIBUTE, Boolean.TRUE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> proceeded.set(true);

        buildFilter().doFilter(request, response, chain);

        assertThat(proceeded).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @SuppressWarnings("unchecked")
    private ApiAccessControlFilter buildFilter() {
        ObjectProvider<ISysAuditLogService> auditLogProvider = Mockito.mock(ObjectProvider.class);
        return new ApiAccessControlFilter(permissionService, auditLogProvider, metrics, properties);
    }

    private static void authenticate() {
        SecurityUser principal = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .username("alice")
                .roles(Set.of())
                .permissions(Set.of())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
