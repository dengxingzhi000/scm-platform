package com.scmcloud.common.security.handler;

import com.scmcloud.common.security.util.TraceIdUtil;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.util.HtmlUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Security-focused REST handler to provide trace-aware JSON errors without colliding with core handler.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityRestExceptionHandler {

    /**
     * Maximum length for sanitized messages to prevent log flooding and response bloat.
     */
    private static final int MAX_MESSAGE_LENGTH = 500;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> sanitizeMessage(err.getField()) + " " + sanitizeMessage(err.getDefaultMessage()))
                .orElse("Validation failed");
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex,
                                                                  HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", sanitizeMessage(ex.getMessage()), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex,
                                                             HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", sanitizeMessage(ex.getMessage()), request);
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
