package com.scmcloud.common.security.util;

import com.scmcloud.common.util.UUIDv7Util;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Trace ID utility for extracting and validating trace IDs from HTTP requests.
 *
 * @author Deng
 * @since 2025-11-27
 */
@UtilityClass
public class TraceIdUtil {
    private static final String HEADER_X_REQUEST_ID = "X-Request-ID";
    private static final String HEADER_TRACEPARENT = "traceparent";

    /**
     * Pattern for validating trace IDs - only allows alphanumeric, hyphens, and underscores.
     * Prevents XSS and header injection attacks via malicious trace ID headers.
     */
    private static final Pattern VALID_TRACE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_]{1,128}$");

    /**
     * Resolve and sanitize trace ID from HTTP request.
     * Order: X-Request-ID → traceparent → generated UUIDv7
     *
     * @param request HTTP request
     * @return sanitized trace ID, never null
     */
    public String resolveAndSanitizeTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(HEADER_X_REQUEST_ID);
        if (sanitizeTraceId(traceId) != null) {
            return traceId;
        }
        traceId = request.getHeader(HEADER_TRACEPARENT);
        if (sanitizeTraceId(traceId) != null) {
            return traceId;
        }
        return UUIDv7Util.generate().toString();
    }

    /**
     * Sanitize trace ID format.
     * Only allows alphanumeric characters, hyphens, and underscores (max 128 chars).
     *
     * @param traceId raw trace ID from header
     * @return sanitized trace ID, or null if invalid/empty
     */
    private String sanitizeTraceId(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            return null;
        }
        if (VALID_TRACE_ID_PATTERN.matcher(traceId).matches()) {
            return traceId;
        }
        return null;
    }
}
