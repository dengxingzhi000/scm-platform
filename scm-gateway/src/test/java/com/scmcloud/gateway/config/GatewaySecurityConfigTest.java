package com.scmcloud.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.web.server.MockWebSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.security.web.server.authentication.logout.LogoutWebFilter;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GatewaySecurityConfigTest {
    private static final String TOKEN = "valid-header-token";
    private static final String API = "/system/api/system/users";
    private static final String SESSION_CONTEXT =
            WebSessionServerSecurityContextRepository.DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME;

    private ReactiveJwtDecoder decoder;
    private SecurityWebFilterChain securityChain;
    private WebFilterChainProxy proxy;
    private AtomicReference<Principal> principal;

    @BeforeEach
    void setUp() {
        decoder = mock(ReactiveJwtDecoder.class);
        securityChain = new GatewaySecurityConfig().springSecurityFilterChain(ServerHttpSecurity.http(), decoder);
        proxy = new WebFilterChainProxy(securityChain);
        principal = new AtomicReference<>();
    }

    @Test
    void cookieOnlyApiRequestIsRejected() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(API)
                .cookie(new HttpCookie("access_token", TOKEN), new HttpCookie("refresh_token", TOKEN),
                        new HttpCookie("SESSION", "browser-session")));

        filter(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(principal.get()).isNull();
        verifyNoInteractions(decoder);
    }

    @Test
    void storedWebSessionAuthenticationCannotAuthenticateApi() {
        MockWebSession session = authenticatedSession();
        Object stored = session.getAttribute(SESSION_CONTEXT);
        MockServerWebExchange exchange = MockServerWebExchange.builder(MockServerHttpRequest.post(API)
                        .cookie(new HttpCookie("SESSION", session.getId())))
                .session(session).build();

        filter(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(principal.get()).isNull();
        assertThat(session.isExpired()).isFalse();
        assertThat((Object) session.getAttribute(SESSION_CONTEXT)).isSameAs(stored);
        verifyNoInteractions(decoder);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bearerAuthenticatesWithoutCsrfAndNeverWritesSessionAuthentication(boolean existingSession) {
        MockWebSession session = existingSession ? authenticatedSession() : new MockWebSession();
        Object stored = session.getAttribute(SESSION_CONTEXT);
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue(TOKEN).header("alg", "HS512").subject("bearer-user")
                .claim("roles", List.of("ADMIN")).issuedAt(now).expiresAt(now.plusSeconds(60)).build();
        when(decoder.decode(TOKEN)).thenReturn(Mono.just(jwt));
        MockServerWebExchange exchange = MockServerWebExchange.builder(MockServerHttpRequest.post(API)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .session(session).build();

        filter(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(principal.get()).isInstanceOfSatisfying(JwtAuthenticationToken.class, authentication -> {
            assertThat(authentication.getName()).isEqualTo("bearer-user");
            assertThat(authentication.getAuthorities()).extracting("authority")
                    .containsExactlyInAnyOrder("FACTOR_BEARER", "ROLE_ADMIN");
        });
        assertThat(session.isExpired()).isFalse();
        assertThat((Object) session.getAttribute(SESSION_CONTEXT)).isSameAs(stored);
        assertThat(session.isStarted()).isEqualTo(existingSession);
        verify(decoder).decode(TOKEN);
    }

    @Test
    void chainHasNoDefaultLogoutWebFilter() {
        StepVerifier.create(securityChain.getWebFilters().collectList())
                .assertNext(filters -> assertThat(filters).noneMatch(LogoutWebFilter.class::isInstance))
                .verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    void logoutDoesNotClearStoredBrowserAuthentication(String method) {
        MockWebSession session = authenticatedSession();
        Object stored = session.getAttribute(SESSION_CONTEXT);
        MockServerWebExchange exchange = MockServerWebExchange.builder(
                        MockServerHttpRequest.method(HttpMethod.valueOf(method), "/logout"))
                .session(session).build();

        filter(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(session.isExpired()).isFalse();
        assertThat((Object) session.getAttribute(SESSION_CONTEXT)).isSameAs(stored);
        verifyNoInteractions(decoder);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/auth/api/auth/login", "/auth/api/auth/refresh", "/auth/oauth2/token",
            "/auth/api/public/probe", "/actuator/health", "/actuator/info"})
    void existingPublicRoutesRemainPermittedWithoutCsrf(String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(path));

        filter(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(principal.get()).isNull();
        verifyNoInteractions(decoder);
    }

    private MockWebSession authenticatedSession() {
        MockWebSession session = new MockWebSession();
        session.start();
        session.getAttributes().put(SESSION_CONTEXT, new SecurityContextImpl(
                new TestingAuthenticationToken("browser-user", null, "ROLE_USER")));
        return session;
    }

    private void filter(MockServerWebExchange exchange) {
        StepVerifier.create(proxy.filter(exchange, filtered -> filtered.getPrincipal()
                        .doOnNext(principal::set)
                        .then(Mono.fromRunnable(() -> filtered.getResponse().setStatusCode(HttpStatus.OK)))))
                .verifyComplete();
    }
}
