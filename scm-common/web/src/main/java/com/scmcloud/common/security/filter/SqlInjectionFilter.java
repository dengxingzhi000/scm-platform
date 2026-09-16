package com.scmcloud.common.security.filter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.scmcloud.common.security.config.SecurityFilterProperties;
import com.scmcloud.common.security.metrics.SecurityMetrics;
import com.scmcloud.common.security.util.IpUtils;
import com.scmcloud.common.security.util.SecurityErrorResponseWriter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import cn.hutool.core.util.StrUtil;

/**
 * SQL/XSS 杞婚噺鍛婅杩囨护鍣細榛樿浠呭憡璀︼紝渚濊禆鎸佷箙灞傚弬鏁板寲闃叉姢锟?
 */
@Component
@Slf4j
public class SqlInjectionFilter implements Filter {
    private static final String ALERT_HEADER = "X-Sql-Guard";

    private final SecurityFilterProperties properties;
    private final SecurityMetrics securityMetrics;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            """
            (/\\*(?:.|[\\n\\r])*?\\*/)|(?:--[\\s\\S]*?$)|
            (\\bunion\\s+(?:all\\s+)?select\\b)|
            ((?:['"]|\\))\\s*(?:or|and)\\s*(?:true|false|\\d+|[a-zA-Z_][a-zA-Z0-9_]*\\s*=\\s*[a-zA-Z0-9_]+))|
            \\b(drop|truncate|exec|execute|declare)\\b
            """.replaceAll("\\n", ""),
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<script[^>]*?>.*?</script>|javascript:|<iframe.*?>|<img[^>]*?onerror\\s*=", Pattern.CASE_INSENSITIVE
    );

    public SqlInjectionFilter(SecurityFilterProperties properties, SecurityMetrics securityMetrics) {
        this.properties = properties;
        this.securityMetrics = securityMetrics;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (properties != null && !properties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        SecurityFilterProperties.SqlFilterMode mode =
                properties != null ? properties.getMode() : SecurityFilterProperties.SqlFilterMode.ALERT;
        boolean monitorOnly = mode == SecurityFilterProperties.SqlFilterMode.ALERT;

        String uri = httpRequest.getRequestURI();
        if (isExcluded(uri)) {
            chain.doFilter(request, response);
            return;
        }

        if (isMultipart(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest requestToUse = scanJsonBody(httpRequest, httpResponse, uri, monitorOnly);
        if (requestToUse == null) {
            return; // already blocked
        }

        DetectionState paramState = scanParameters(httpRequest, httpResponse, uri, monitorOnly);
        if (paramState == DetectionState.BLOCKED) {
            return;
        }

        chain.doFilter(requestToUse, response);
    }

    private boolean isExcluded(String uri) {
        if (properties == null || properties.getExcludePaths() == null) {
            return false;
        }
        for (String pattern : properties.getExcludePaths()) {
            if (StrUtil.isNotBlank(pattern) && pathMatcher.match(pattern.trim(), uri)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return StrUtil.isNotBlank(contentType) && StrUtil.startWithIgnoreCase(contentType, "multipart/");
    }

    private HttpServletRequest scanJsonBody(HttpServletRequest request, HttpServletResponse response, String uri,
                                            boolean monitorOnly) throws IOException {
        String contentType = request.getContentType();
        if (StrUtil.isBlank(contentType) || !StrUtil.startWithIgnoreCase(contentType, "application/json")
                || !StrUtil.equalsAnyIgnoreCase(request.getMethod(), "POST", "PUT", "PATCH")) {
            return request;
        }

        int maxScanBytes = 64 * 1024;
        int contentLength = request.getContentLength();
        if (contentLength >= 0 && contentLength > maxScanBytes) {
            return request;
        }

        byte[] bodyBytes = readLimited(request.getInputStream(), maxScanBytes);

        if (bodyBytes.length <= maxScanBytes) {
            String bodyStr = new String(bodyBytes, StandardCharsets.UTF_8);
            if (StrUtil.isNotBlank(bodyStr)) {
                try {
                    Object json = JSON.parse(bodyStr);
                    DetectionState state = containsMaliciousInJson(json, request, response, uri, monitorOnly);
                    if (state == DetectionState.BLOCKED) {
                        return null;
                    }
                } catch (Exception ignored) {
                    // 闈炰弗锟絁SON 鎴栬В鏋愬け璐ワ紝璺宠繃 JSON 浣撴壂锟?
                }
            }
        }
        return new CachedBodyHttpServletRequest(request, bodyBytes);
    }

    private DetectionState scanParameters(HttpServletRequest request, HttpServletResponse response, String uri,
                                          boolean monitorOnly) throws IOException {
        for (String paramName : request.getParameterMap().keySet()) {
            String[] paramValues = request.getParameterValues(paramName);
            if (paramValues == null) continue;

            for (String paramValue : paramValues) {
                if (StrUtil.isBlank(paramValue)) continue;
                String preview = generatePreview(paramValue);

                if (SQL_INJECTION_PATTERN.matcher(paramValue).find()) {
                    log.warn("SQL injection detected! mode={}, traceId={}, param={}, preview={}, ip={}, uri={}",
                            monitorOnly ? "ALERT" : "BLOCK",
                            request.getHeader("X-Request-ID"), paramName, preview,
                            IpUtils.getClientIp(request), uri);
                    DetectionState state = handleDetection(request, response,
                            "SQL_INJECTION", "妫€娴嬪埌鐤戜技 SQL 娉ㄥ叆", monitorOnly, true);
                    if (state == DetectionState.BLOCKED) return state;
                    return state;
                }

                if ((properties == null || properties.isXssEnabled()) && XSS_PATTERN.matcher(paramValue).find()) {
                    log.warn("XSS attack detected! mode={}, traceId={}, param={}, preview={}, ip={}, uri={}",
                            monitorOnly ? "ALERT" : "BLOCK",
                            request.getHeader("X-Request-ID"), paramName, preview,
                            IpUtils.getClientIp(request), uri);
                    DetectionState state = handleDetection(request, response,
                            "XSS_ATTACK", "妫€娴嬪埌鐤戜技 XSS 鏀诲嚮", monitorOnly, false);
                    if (state == DetectionState.BLOCKED) return state;
                    return state;
                }
            }
        }
        return DetectionState.NONE;
    }

    private DetectionState containsMaliciousInJson(Object node, HttpServletRequest req, HttpServletResponse resp,
                                                   String uri, boolean monitorOnly) throws IOException {
        switch (node) {
            case null -> {
                return DetectionState.NONE;
            }
            case JSONObject obj -> {
                for (String key : obj.keySet()) {
                    Object val = obj.get(key);
                    if (val instanceof CharSequence cs) {
                        String s = cs.toString();
                        DetectionState state = isMalicious(s, key, uri, req, resp, monitorOnly);
                        if (state != DetectionState.NONE) {
                            return state;
                        }
                    } else {
                        DetectionState nested = containsMaliciousInJson(val, req, resp, uri, monitorOnly);
                        if (nested != DetectionState.NONE) {
                            return nested;
                        }
                    }
                }
            }
            case JSONArray arr -> {
                for (Object val : arr) {
                    DetectionState nested = containsMaliciousInJson(val, req, resp, uri, monitorOnly);
                    if (nested != DetectionState.NONE) {
                        return nested;
                    }
                }
            }
            default -> {
                // 鍏朵粬绫诲瀷涓嶅锟?
            }
        }
        return DetectionState.NONE;
    }

    private DetectionState isMalicious(String value, String field, String uri, HttpServletRequest req,
                                       HttpServletResponse resp, boolean monitorOnly) throws IOException {
        if (StrUtil.isBlank(value)) return DetectionState.NONE;

        if (SQL_INJECTION_PATTERN.matcher(value).find()) {
            String preview = generatePreview(value);
            log.warn("SQL injection detected in JSON! mode={}, traceId={}, field={}, preview={}, ip={}, uri={}",
                    monitorOnly ? "ALERT" : "BLOCK",
                    req.getHeader("X-Request-ID"), field, preview, IpUtils.getClientIp(req), uri);
            return handleDetection(req, resp, "SQL_INJECTION", "妫€娴嬪埌鐤戜技 SQL 娉ㄥ叆", monitorOnly, true);
        }

        if ((properties == null || properties.isXssEnabled()) && XSS_PATTERN.matcher(value).find()) {
            String preview = generatePreview(value);
            log.warn("XSS attack detected in JSON! mode={}, traceId={}, field={}, preview={}, ip={}, uri={}",
                    monitorOnly ? "ALERT" : "BLOCK",
                    req.getHeader("X-Request-ID"), field, preview, IpUtils.getClientIp(req), uri);
            return handleDetection(req, resp, "XSS_ATTACK", "妫€娴嬪埌鐤戜技 XSS 鏀诲嚮", monitorOnly, false);
        }

        return DetectionState.NONE;
    }

    private DetectionState handleDetection(HttpServletRequest request, HttpServletResponse response, String code,
                                           String message, boolean monitorOnly, boolean isSql) throws IOException {
        securityMetrics.increment(isSql ? "security.sql.detected" : "security.xss.detected");
        if (!monitorOnly) {
            securityMetrics.increment(isSql ? "security.sql.blocked" : "security.xss.blocked");
            SecurityErrorResponseWriter.write(request, response, HttpServletResponse.SC_FORBIDDEN, code, message);
            return DetectionState.BLOCKED;
        }
        if (response.getHeader(ALERT_HEADER) == null) {
            response.setHeader(ALERT_HEADER, code);
        }
        return DetectionState.ALERTED;
    }

    private String generatePreview(String value) {
        return value.length() > 64 ? (value.substring(0, 64) + "..." + "(len=" + value.length() + ")") : value;
    }

    private byte[] readLimited(InputStream inputStream, int maxBytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.min(4096, maxBytes));
        byte[] buffer = new byte[4096];
        int total = 0;
        int n;
        while ((n = inputStream.read(buffer, 0, Math.min(buffer.length, maxBytes - total))) > 0) {
            baos.write(buffer, 0, n);
            total += n;
            if (total >= maxBytes) {
                break;
            }
        }
        return baos.toByteArray();
    }

    private enum DetectionState {
        NONE,
        ALERTED,
        BLOCKED
    }
}
