package com.scmcloud.common.handler;

import com.scmcloud.common.security.handler.SecurityRestExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This advice is ordered above {@code GlobalExceptionHandler}, so whatever it matches first wins.
 * A failed login used to fall into the {@code Exception} catch-all and surface as a 500, which
 * left the SPA unable to tell "wrong password" from a broken backend and unable to refresh.
 */
class SecurityRestExceptionHandlerTest {

    private final SecurityRestExceptionHandler handler = new SecurityRestExceptionHandler();

    private HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Request-ID")).thenReturn("trace-test");
        return request;
    }

    @Test
    void failedAuthenticationIsUnauthorizedNotServerError() {
        ResponseEntity<Map<String, Object>> response = handler.handleAuthentication(
                new BadCredentialsException("Invalid username or password, attempts remaining: 4"), request());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().get("code"));
        assertEquals("UNAUTHENTICATED", response.getBody().get("error"));
        assertEquals("Invalid username or password, attempts remaining: 4", response.getBody().get("message"));
    }

    @Test
    void accessDeniedKeepsThePrincipalAndAuthorityOutOfTheResponse() {
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(
                new AccessDeniedException("Access Denied for user [SystemUser [username=alice, [ROLE_ADMIN]]]"),
                request());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Access denied", response.getBody().get("message"));
    }

    /**
     * This advice matched {@code Exception} once, which meant every failure the application did not author
     * came back as text the client must not see — and every business exception lost its status and code.
     */
    @Test
    void noCatchAllSoUnexpectedFailuresFallThroughToTheCoreAdvice() {
        Set<Class<? extends Throwable>> handled = Arrays.stream(SecurityRestExceptionHandler.class.getDeclaredMethods())
                .map(m -> m.getAnnotation(ExceptionHandler.class))
                .filter(Objects::nonNull)
                .flatMap(a -> Arrays.stream(a.value()))
                .collect(Collectors.toSet());

        assertFalse(handled.contains(Exception.class), "catch-all would shadow GlobalExceptionHandler");
        assertTrue(handled.contains(AuthenticationException.class));
    }
}
