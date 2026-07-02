package com.scmcloud.gateway.security;

import com.scmcloud.gateway.properties.IdentityPropagationProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdentityPropagationWebFilter Tests")
class IdentityPropagationWebFilterTest {

    @Mock
    private IdentityTokenEncoder tokenEncoder;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private WebFilterChain chain;

    @Mock
    private Counter propagationCounter;

    @Mock
    private Counter skipCounter;

    @InjectMocks
    private IdentityPropagationWebFilter filter;

    private IdentityPropagationProperties properties;
    private MockServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        properties = new IdentityPropagationProperties();
        properties.setEnabled(true);
        properties.setIdentityTokenHeader("X-Identity-Token");
        properties.setUserIdHeader("X-User-Id");
        properties.setUsernameHeader("X-User-Name");
        properties.setDeviceIdHeader("X-Device-Id");
        properties.setRolesHeader("X-User-Roles");
        properties.setUserIdClaim("userId");
        properties.setUsernameClaim("username");
        properties.setDeviceIdClaim("deviceId");

        filter = new IdentityPropagationWebFilter(properties, tokenEncoder, meterRegistry);

        when(meterRegistry.counter("gateway.identity.propagation.success")).thenReturn(propagationCounter);
        when(meterRegistry.counter("gateway.identity.propagation.skip")).thenReturn(skipCounter);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        exchange = MockServerWebExchange.from(request);

        when(chain.filter(any())).thenReturn(Mono.empty());
        when(tokenEncoder.encode(any())).thenReturn("signed-token");
    }

    @Test
    @DisplayName("Should skip when filter is disabled")
    void testFilter_Disabled() {
        properties.setEnabled(false);
        filter = new IdentityPropagationWebFilter(properties, tokenEncoder, meterRegistry);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(tokenEncoder);
    }

    @Test
    @DisplayName("Should propagate identity headers when authenticated")
    void testFilter_PropagateIdentity() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("sub-123")
                .claim("userId", "user-001")
                .claim("username", "admin")
                .claim("deviceId", "device-001")
                .issuedAt(Instant.now())
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt,
                List.of(() -> "ROLE_USER", () -> "ROLE_ADMIN"));

        SecurityContextImpl context = new SecurityContextImpl(auth);

        StepVerifier.create(
                filter.filter(exchange, chain)
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                                Mono.just(context))))
                .verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        ServerWebExchange captured = captor.getValue();
        assertThat(captured.getRequest().getHeaders().getFirst("X-Identity-Token"))
                .isEqualTo("signed-token");
        assertThat(captured.getRequest().getHeaders().getFirst("X-User-Id"))
                .isEqualTo("user-001");
        assertThat(captured.getRequest().getHeaders().getFirst("X-User-Name"))
                .isEqualTo("admin");
        assertThat(captured.getRequest().getHeaders().getFirst("X-Device-Id"))
                .isEqualTo("device-001");
        assertThat(captured.getRequest().getHeaders().getFirst("X-User-Roles"))
                .isEqualTo("ROLE_USER,ROLE_ADMIN");

        verify(propagationCounter).increment();
    }

    @Test
    @DisplayName("Should skip when no security context")
    void testFilter_NoSecurityContext() {
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(skipCounter).increment();
        verifyNoInteractions(tokenEncoder);
    }

    @Test
    @DisplayName("Should skip when authentication is not JWT")
    void testFilter_NonJwtAuthentication() {
        TestAuthenticationToken auth = new TestAuthenticationToken("user", "pass",
                List.of(() -> "ROLE_USER"));

        SecurityContextImpl context = new SecurityContextImpl(auth);

        StepVerifier.create(
                filter.filter(exchange, chain)
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                                Mono.just(context))))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(skipCounter).increment();
        verifyNoInteractions(tokenEncoder);
    }

    @Test
    @DisplayName("Should skip when userId claim is missing")
    void testFilter_MissingUserId() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("sub-123")
                .claim("username", "admin")
                .issuedAt(Instant.now())
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt,
                List.of(() -> "ROLE_USER"));

        SecurityContextImpl context = new SecurityContextImpl(auth);

        StepVerifier.create(
                filter.filter(exchange, chain)
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                                Mono.just(context))))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(skipCounter).increment();
        verifyNoInteractions(tokenEncoder);
    }

    @Test
    @DisplayName("Should return correct order")
    void testGetOrder() {
        assertThat(filter.getOrder()).isLessThan(0);
    }
}
