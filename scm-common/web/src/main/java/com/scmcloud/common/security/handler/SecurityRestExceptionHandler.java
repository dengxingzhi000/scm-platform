package com.scmcloud.common.security.handler;

import com.scmcloud.common.security.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Security-focused REST handler to provide trace-aware JSON errors without colliding with core handler.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class SecurityRestExceptionHandler {

    /**
     * Maximum length for sanitized messages to prevent log flooding and response bloat.
     */
    private static final int MAX_MESSAGE_LENGTH = 500;

    /**
     * Advice beans are matched by type across the whole hierarchy, so an {@code Exception} catch-all here
     * would shadow every handler in {@code GlobalExceptionHandler}, including its own: business exceptions
     * would lose their status and code, and any unexpected failure would be echoed back as text.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> sanitizeMessage(err.getField()) + " " + sanitizeMessage(err.getDefaultMessage()))
                .orElse("Validation failed");
        logFailure(HttpStatus.BAD_REQUEST, request, message);
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex,
                                                                  HttpServletRequest request) {
        // Spring Security puts the principal and the missing authority into this message.
        logFailure(HttpStatus.FORBIDDEN, request, ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied", request);
    }

    // The SPA reads these messages to tell "wrong password" from a broken backend and to detect the
    // two-factor prompt, so they stay; dropping this handler would send failed logins to the 500 path.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex,
                                                                    HttpServletRequest request) {
        logFailure(HttpStatus.UNAUTHORIZED, request, ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", sanitizeMessage(ex.getMessage()), request);
    }

    private void logFailure(HttpStatus status, HttpServletRequest request, String detail) {
        String traceId = TraceIdUtil.resolveAndSanitizeTraceId(request);
        if (status.is4xxClientError()) {
            log.warn("{} {} rejected, traceId={}: {}", status.value(), request.getRequestURI(), traceId, detail);
        } else {
            log.error("{} {} failed, traceId={}: {}", status.value(), request.getRequestURI(), traceId, detail);
        }
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String sanitizedMessage,
                                                              HttpServletRequest request) {
        String traceId = TraceIdUtil.resolveAndSanitizeTraceId(request);

        Map<String, Object> body = buildSafeResponseBody(
                status.value(),
                error,
                sanitizedMessage,
                traceId,
                request.getRequestURI()
        );

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Request-ID", traceId)
                .body(body);
    }

    /**
     * Builds the response body with all values properly sanitized.
     */
    private static Map<String, Object> buildSafeResponseBody(int code, String error, String message, String traceId,
                                                             String rawPath) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("error", error);
        body.put("message", message);
        body.put("traceId", traceId);
        body.put("path", encodePath(rawPath));

        return body;
    }

    /**
     * URL-encodes the path to neutralize any potentially dangerous characters.
     */
    private static String encodePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return URLEncoder.encode(path, StandardCharsets.UTF_8);
    }

    /**
     * Sanitizes error messages to prevent XSS.
     */
    private String sanitizeMessage(String message) {
        if (message == null) {
            return "An error occurred";
        }
        String sanitized = HtmlUtils.htmlEscape(message);
        if (sanitized.length() > MAX_MESSAGE_LENGTH) {
            sanitized = sanitized.substring(0, MAX_MESSAGE_LENGTH) + "...";
        }
        return sanitized;
    }
}
