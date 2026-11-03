package com.scmcloud.auth.security;

import com.scmcloud.common.security.config.SecurityConfig;
import com.scmcloud.common.security.config.SecurityFilterProperties;
import com.scmcloud.common.security.config.SecurityHeadersProperties;
import com.scmcloud.common.security.filter.JwtAuthenticationFilter;
import com.scmcloud.common.security.filter.SqlInjectionFilter;
import com.scmcloud.common.security.handler.JwtAccessDeniedHandler;
import com.scmcloud.common.security.handler.JwtAuthenticationEntryPoint;
import com.scmcloud.common.security.metrics.SecurityMetrics;
import com.scmcloud.common.security.properties.JwtProperties;
import com.scmcloud.common.security.util.HttpServletRequestUtils;
import com.scmcloud.common.security.util.JwtUtils;
import com.scmcloud.common.uaa.config.AuthorizationServerConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CsrfBoundaryTest {
    private static final String TOKEN = "valid-header-token";
    private static final String API = "/api/private/probe";
    private static final String SESSION_CONTEXT = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private JwtUtils jwtUtils;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (context != null) {
            context.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void apiRejectsCookieOnlyAuthentication(boolean uaaEnabled) throws Exception {
        startContext(uaaEnabled);

        mvc.perform(post(API).cookie(new Cookie("access_token", TOKEN),
                        new Cookie("refresh_token", TOKEN), new Cookie("JSESSIONID", "browser-session")))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(jwtUtils);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void apiRejectsStoredSessionAuthentication(boolean uaaEnabled) throws Exception {
        startContext(uaaEnabled);
        MockHttpSession session = authenticatedSession();

        mvc.perform(post(API).session(session))
                .andExpect(status().isUnauthorized());

        assertSessionIntact(session);
        verifyNoInteractions(jwtUtils);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void apiAcceptsBearerWithoutCsrfAndDoesNotPersistAuthentication(boolean uaaEnabled) throws Exception {
        startContext(uaaEnabled);

        MvcResult result = mvc.perform(post(API).header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("bearer-user"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
        verify(jwtUtils).validateToken(eq(TOKEN), anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE"})
    void statelessLogoutNeverInvalidatesAnExistingSession(String method) throws Exception {
        startContext(false);
        MockHttpSession session = authenticatedSession();

        assertThat(context.getBean("securityFilterChain", SecurityFilterChain.class).getFilters())
                .noneMatch(LogoutFilter.class::isInstance);

        mvc.perform(request(HttpMethod.valueOf(method), "/logout").session(session))
                .andExpect(status().isUnauthorized());
        assertSessionIntact(session);

        mvc.perform(request(HttpMethod.valueOf(method), "/logout").session(session)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("bearer-user"));
        assertSessionIntact(session);
    }

    @Test
    void oauthGetLogoutOnlyRendersConfirmation() throws Exception {
        startContext(true);
        MockHttpSession session = authenticatedSession();

        mvc.perform(get("/logout").session(session))
                .andExpect(status().isOk());

        assertSessionIntact(session);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    void oauthPostLogoutRejectsMissingAndInvalidCsrf() throws Exception {
        startContext(true);
        MockHttpSession session = authenticatedSession();

        mvc.perform(post("/logout").session(session))
                .andExpect(status().isForbidden());
        assertSessionIntact(session);

        mvc.perform(post("/logout").session(session).param("_csrf", "invalid"))
                .andExpect(status().isForbidden());
        assertSessionIntact(session);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    void oauthPostLogoutWithSessionCsrfInvalidatesSession() throws Exception {
        startContext(true);
        MockHttpSession session = authenticatedSession();
        MvcResult confirmation = mvc.perform(get("/logout").session(session))
                .andExpect(status().isOk())
                .andReturn();
        CsrfToken csrf = (CsrfToken) confirmation.getRequest().getAttribute(CsrfToken.class.getName());
        assertThat(csrf).isNotNull();
        assertSessionIntact(session);

        mvc.perform(post("/logout").session(session).param(csrf.getParameterName(), csrf.getToken()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?logout"));

        assertThat(session.isInvalid()).isTrue();
        verifyNoInteractions(jwtUtils);
    }

    private void startContext(boolean uaaEnabled) {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                Map.of("security.uaa.enabled", Boolean.toString(uaaEnabled))));
        context.register(TestConfig.class, AuthorizationServerConfig.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jwtUtils = context.getBean(JwtUtils.class);
        clearInvocations(jwtUtils);
    }

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession(context.getServletContext());
        session.setAttribute(SESSION_CONTEXT, new SecurityContextImpl(
                new TestingAuthenticationToken("browser-user", null, "ROLE_USER")));
        return session;
    }

    private void assertSessionIntact(MockHttpSession session) {
        assertThat(session.isInvalid()).isFalse();
        SecurityContext stored = (SecurityContext) session.getAttribute(SESSION_CONTEXT);
        assertThat(stored).isNotNull();
        assertThat(stored.getAuthentication()).isNotNull();
        assertThat(stored.getAuthentication().isAuthenticated()).isTrue();
        assertThat(stored.getAuthentication().getName()).isEqualTo("browser-user");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({SecurityConfig.class, ProbeController.class})
    static class TestConfig {
        @Bean
        JwtUtils jwtUtils() {
            JwtUtils utils = mock(JwtUtils.class);
            when(utils.validateToken(eq(TOKEN), anyString(), anyString())).thenReturn(true);
            when(utils.getUserIdFromToken(TOKEN)).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
            when(utils.getUsernameFromToken(TOKEN)).thenReturn("bearer-user");
            when(utils.getPermissionsFromToken(TOKEN)).thenReturn(Set.of("api:read"));
            when(utils.getRolesFromToken(TOKEN)).thenReturn(Set.of("ROLE_USER"));
            return utils;
        }

        @Bean
        SecurityMetrics securityMetrics() {
            return mock(SecurityMetrics.class);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtils utils, SecurityMetrics metrics) {
            return new JwtAuthenticationFilter(utils, new HttpServletRequestUtils(new JwtProperties()), metrics);
        }

        @Bean
        SqlInjectionFilter sqlInjectionFilter(SecurityMetrics metrics) {
            return new SqlInjectionFilter(new SecurityFilterProperties(), metrics);
        }

        @Bean
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
            return new JwtAuthenticationEntryPoint();
        }

        @Bean
        JwtAccessDeniedHandler jwtAccessDeniedHandler() {
            return new JwtAccessDeniedHandler();
        }

        @Bean
        SecurityHeadersProperties securityHeadersProperties() {
            return new SecurityHeadersProperties();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(User.withUsername("browser-user")
                    .password("unused").roles("USER").build());
        }
    }

    @RestController
    static class ProbeController {
        @RequestMapping({API, "/logout"})
        String principal(Principal principal) {
            return principal.getName();
        }
    }
}
