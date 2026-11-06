package com.cognora.platform.common.rest.signature;

import com.cognora.platform.common.security.PermissionService;
import com.cognora.platform.common.security.config.ApiAccessControlProperties;
import com.cognora.platform.common.security.filter.InternalEndpointSignatureFilter;
import com.cognora.platform.common.security.metrics.SecurityMetrics;
import com.cognora.platform.common.web.domain.SecurityUser;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalEndpointSignatureFilterTest {

    private static final String SECRET = "unit-test-internal-secret-0123456789abcdef";

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsAnUnsignedRequestToAConfiguredPath() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);

        filter(properties()).doFilter(
                new MockHttpServletRequest("POST", "/api/v1/inventory/reservation"),
                response, chain(proceeded));

        assertThat(proceeded).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("ACCESS_DENIED");
        assertThat(response.getContentAsString())
                .contains("Internal endpoint requires a valid service signature");
        assertThat(meterRegistry.counter(InternalEndpointSignatureFilter.METRIC_REJECTED).count())
                .isEqualTo(1.0);
    }

    @Test
    void rejectsAWrongSignature() throws Exception {
        MockHttpServletRequest request = InternalSignatureVerifierTest.signedRequest(
                System.currentTimeMillis(), "nonce-filter", null);
        request.removeHeader("X-Signature");
        request.addHeader("X-Signature", "deadbeef");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);

        filter(properties()).doFilter(request, response, chain(proceeded));

        assertThat(proceeded).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void rejectsAStaleTimestamp() throws Exception {
        MockHttpServletRequest request = InternalSignatureVerifierTest.signedRequest(
                System.currentTimeMillis() - 6 * 60 * 1000L, "nonce-stale", null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);

        filter(properties()).doFilter(request, response, chain(proceeded));

        assertThat(proceeded).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void letsAValidSignatureThrough() throws Exception {
        MockHttpServletRequest request = InternalSignatureVerifierTest.signedRequest(
                System.currentTimeMillis(), "nonce-ok", null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);

        filter(properties()).doFilter(request, response, chain(proceeded));

        assertThat(proceeded).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(meterRegistry.counter(InternalEndpointSignatureFilter.METRIC_REJECTED).count())
                .isZero();
    }

    @Test
    void ignoresPathsOutsideTheConfiguredList() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);

        filter(properties()).doFilter(
                new MockHttpServletRequest("GET", "/api/v1/orders"),
                response, chain(proceeded));

        assertThat(proceeded).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void matrixParametersDoNotBypassTheCheck() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);

        filter(properties()).doFilter(
                new MockHttpServletRequest("POST", "/api/v1/inventory/reservation;x=1"),
                response, chain(proceeded));

        assertThat(proceeded).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void exactPathsAreMatchedWithEqualsSemantics() throws Exception {
        ApiAccessControlProperties properties = new ApiAccessControlProperties();
        properties.setInternalSignaturePaths(List.of("/api/v1/payments"));
        AtomicBoolean proceeded = new AtomicBoolean(false);

        filter(properties).doFilter(
                new MockHttpServletRequest("POST", "/api/v1/payments/create"),
                new MockHttpServletResponse(), chain(proceeded));

        assertThat(proceeded).isTrue();
    }

    @Test
    void dualIdentityAcceptsAUserHoldingThePermission() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, Set.of());
        PermissionService permissionService = mock(PermissionService.class);
        when(permissionService.findAllPermissionsByUserId(userId))
                .thenReturn(Set.of("search:index:manage"));
        AtomicBoolean proceeded = new AtomicBoolean(false);

        filter(properties(), permissionService).doFilter(
                new MockHttpServletRequest("POST", "/api/v1/search/index/reindex"),
                new MockHttpServletResponse(), chain(proceeded));

        assertThat(proceeded).isTrue();
    }

    @Test
    void dualIdentityRejectsAUserWithoutThePermission() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, Set.of("search:product:view"));
        PermissionService permissionService = mock(PermissionService.class);
        when(permissionService.findAllPermissionsByUserId(userId))
                .thenReturn(Set.of("search:product:view"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean proceeded = new AtomicBoolean(false);

        filter(properties(), permissionService).doFilter(
                new MockHttpServletRequest("POST", "/api/v1/search/index/reindex"),
                response, chain(proceeded));

        assertThat(proceeded).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("search:index:manage");
    }

    @Test
    void dualIdentityAcceptsAValidServiceSignature() throws Exception {
        MockHttpServletRequest request = InternalSignatureVerifierTest.signedRequest(
                System.currentTimeMillis(), "nonce-dual", null, "/api/v1/search/index/reindex");
        AtomicBoolean proceeded = new AtomicBoolean(false);

        filter(properties(), mock(PermissionService.class)).doFilter(
                request, new MockHttpServletResponse(), chain(proceeded));

        assertThat(proceeded).isTrue();
        assertThat(request.getAttribute(InternalEndpointSignatureFilter.SIGNATURE_VERIFIED_ATTRIBUTE))
                .isEqualTo(Boolean.TRUE);
    }

    @Test
    void dualIdentityRejectsWhenNeitherSignatureNorPermissionIsPresent() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);
        AtomicBoolean proceeded = new AtomicBoolean(false);

        filter(properties(), permissionService).doFilter(
                new MockHttpServletRequest("POST", "/api/v1/search/index/reindex"),
                new MockHttpServletResponse(), chain(proceeded));

        assertThat(proceeded).isFalse();
    }

    @Test
    void dualIdentityUsesPermissionLookupWhenPrincipalIsNotASecurityUser() throws Exception {
        UUID userId = UUID.randomUUID();
        // Resource-server style principal: the userId claim resolves, but getCurrentUser() is null.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new DefaultOAuth2AuthenticatedPrincipal(
                                "bearer-user", Map.of("userId", userId.toString()), List.of()),
                        null, List.of()));
        PermissionService permissionService = mock(PermissionService.class);
        when(permissionService.findAllPermissionsByUserId(userId))
                .thenReturn(Set.of("search:index:manage"));
        AtomicBoolean proceeded = new AtomicBoolean(false);

        filter(properties(), permissionService).doFilter(
                new MockHttpServletRequest("POST", "/api/v1/search/index/reindex"),
                new MockHttpServletResponse(), chain(proceeded));

        assertThat(proceeded).isTrue();
    }

    private InternalEndpointSignatureFilter filter(ApiAccessControlProperties properties) {
        return filter(properties, mock(PermissionService.class));
    }

    private InternalEndpointSignatureFilter filter(ApiAccessControlProperties properties,
                                                   PermissionService permissionService) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("security.signature.app-secrets.internal-service", SECRET);
        InternalSignatureSecretResolver resolver = new InternalSignatureSecretResolver(environment);
        return new InternalEndpointSignatureFilter(
                new InternalSignatureVerifier(resolver),
                new SecurityMetrics(meterRegistry),
                properties,
                permissionService);
    }

    private static void authenticate(UUID userId, Set<String> permissions) {
        SecurityUser user = SecurityUser.builder()
                .userId(userId)
                .username("user-" + userId)
                .permissions(permissions)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private static ApiAccessControlProperties properties() {
        return new ApiAccessControlProperties();
    }

    private static FilterChain chain(AtomicBoolean proceeded) {
        return (request, response) -> proceeded.set(true);
    }
}
