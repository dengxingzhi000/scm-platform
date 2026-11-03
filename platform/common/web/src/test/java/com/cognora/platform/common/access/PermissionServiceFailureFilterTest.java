package com.cognora.platform.common.access;

import com.cognora.platform.common.log.service.ISysAuditLogService;
import com.cognora.platform.common.security.PermissionService;
import com.cognora.platform.common.security.PermissionService.PermissionServiceException;
import com.cognora.platform.common.security.config.ApiAccessControlProperties;
import com.cognora.platform.common.security.filter.ApiAccessControlFilter;
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

class PermissionServiceFailureFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returns503WhenPermissionServiceIsUnavailable() throws Exception {
        PermissionService permissionService = Mockito.mock(PermissionService.class);
        ObjectProvider<ISysAuditLogService> auditLogProvider = mockAuditProvider();
        ApiAccessControlProperties properties = new ApiAccessControlProperties();
        properties.setWhitelist(List.of());

        when(permissionService.findPermissionsByUrl(anyString(), anyString()))
                .thenThrow(new PermissionServiceException("Permission service unavailable via RestClient"));

        ApiAccessControlFilter filter = new ApiAccessControlFilter(
                permissionService, auditLogProvider, new SecurityMetrics(new SimpleMeterRegistry()), properties);
        authenticate();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/secure/data");
        request.addHeader("X-Request-ID", "trace-503");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> proceeded.set(true);

        filter.doFilter(request, response, chain);

        assertThat(proceeded).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("PERMISSION_SERVICE_UNAVAILABLE");
        assertThat(response.getContentAsString()).contains("trace-503");
    }

    @Test
    void stillReturns403WhenUserLacksPermission() throws Exception {
        PermissionService permissionService = Mockito.mock(PermissionService.class);
        ObjectProvider<ISysAuditLogService> auditLogProvider = mockAuditProvider();
        ApiAccessControlProperties properties = new ApiAccessControlProperties();
        properties.setWhitelist(List.of());

        when(permissionService.findPermissionsByUrl(anyString(), anyString()))
                .thenReturn(List.of("perm:read"));
        when(permissionService.findAllPermissionsByUserId(any(UUID.class)))
                .thenReturn(Set.of());

        ApiAccessControlFilter filter = new ApiAccessControlFilter(
                permissionService, auditLogProvider, new SecurityMetrics(new SimpleMeterRegistry()), properties);
        authenticate();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/secure/data");
        request.addHeader("X-Request-ID", "trace-403");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> proceeded.set(true);

        filter.doFilter(request, response, chain);

        assertThat(proceeded).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("ACCESS_DENIED");
    }

    @Test
    void returns503WhenUserPermissionLookupFails() throws Exception {
        PermissionService permissionService = Mockito.mock(PermissionService.class);
        ObjectProvider<ISysAuditLogService> auditLogProvider = mockAuditProvider();
        ApiAccessControlProperties properties = new ApiAccessControlProperties();
        properties.setWhitelist(List.of());

        when(permissionService.findPermissionsByUrl(anyString(), anyString()))
                .thenReturn(List.of("perm:read"));
        when(permissionService.findAllPermissionsByUserId(any(UUID.class)))
                .thenThrow(new PermissionServiceException("Permission service unavailable via RestClient"));

        ApiAccessControlFilter filter = new ApiAccessControlFilter(
                permissionService, auditLogProvider, new SecurityMetrics(new SimpleMeterRegistry()), properties);
        authenticate();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/secure/data");
        request.addHeader("X-Request-ID", "trace-user-503");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> proceeded.set(true);

        filter.doFilter(request, response, chain);

        assertThat(proceeded).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("PERMISSION_SERVICE_UNAVAILABLE");
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ISysAuditLogService> mockAuditProvider() {
        return Mockito.mock(ObjectProvider.class);
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
