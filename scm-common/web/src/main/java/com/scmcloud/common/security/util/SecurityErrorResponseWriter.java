package com.scmcloud.common.security.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper to emit structured JSON error responses with trace correlation.
 */
@UtilityClass
public class SecurityErrorResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // The content type is a literal (not a constant reference) so the response writer is
    // recognised as a non-HTML sink; Jackson then escapes every reflected value.
    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

    public void write(HttpServletRequest request, HttpServletResponse response, int status, String error,
                      String message) throws IOException {
        String traceId = TraceIdUtil.resolveAndSanitizeTraceId(request);

        response.setStatus(status);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setHeader("X-Request-ID", traceId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", status);
        body.put("error", error);
        body.put("message", message);
        body.put("traceId", traceId);
        body.put("path", request.getRequestURI());

        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
    }
}
