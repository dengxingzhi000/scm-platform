package com.cognora.platform.audit.security;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.cognora.platform.common.security.util.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审计写入入口的调用方校验。
 *
 * <p>{@code POST /sys-audit-log} 是 scm-common 的 {@code SysAuditServiceClient} 使用的服务间写入通道，
 * 调用方会携带 {@code RestClientRequestSignatureInterceptor} 生成的 HMAC 头。本过滤器只放行带合法
 * 签名的内部调用，防止任意登录用户伪造/删除审计记录。校验算法与拦截器保持一致：
 * <pre>signature = HMAC-SHA256(secretKey, timestamp + nonce + appId + uri + sortedParams)</pre>
 *
 * <p>未校验 nonce 重放：platform-audit 不依赖 Redis。时间戳窗口限制为 5 分钟。
 */
@Slf4j
@Component
public class AuditWriteRequestGuard extends OncePerRequestFilter {

    static final String WRITE_PATH = "/sys-audit-log";
    private static final long MAX_CLOCK_SKEW_MILLIS = 5 * 60 * 1000L;

    @Value("${security.feign.secret-key:${API_SECRET_INTERNAL_SECRET:your-internal-secret-key}}")
    private String secretKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && WRITE_PATH.equals(pathWithinApplication(request)));
    }

    /**
     * 调用方按 {@code RestClientRequestSignatureInterceptor} 的算法只签 URI 路径（不含 context path），
     * 因此服务端也必须用去掉 context path 的路径，否则加 context path 部署时守卫会整体失效。
     */
    private static String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!hasValidSignature(request)) {
            log.warn("Rejected unsigned audit write: uri={}, remote={}",
                    sanitize(request.getRequestURI()), sanitize(request.getRemoteAddr()));
            SecurityErrorResponseWriter.write(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "ACCESS_DENIED", "Audit writes are restricted to internal services");
            return;
        }
        filterChain.doFilter(request, response);
    }

    boolean hasValidSignature(HttpServletRequest request) {
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String signature = request.getHeader("X-Signature");
        String appId = request.getHeader("X-App-Id");
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)
                || !StringUtils.hasText(signature) || !StringUtils.hasText(appId)) {
            return false;
        }
        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Math.abs(System.currentTimeMillis() - requestTime) > MAX_CLOCK_SKEW_MILLIS) {
            return false;
        }

        String signContent = timestamp + nonce + appId + pathWithinApplication(request)
                + sortAndConcatParams(request.getQueryString());
        String expected = SecureUtil.hmac(HmacAlgorithm.HmacSHA256, secretKey.getBytes(StandardCharsets.UTF_8))
                .digestHex(signContent);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private String sortAndConcatParams(String query) {
        if (!StringUtils.hasText(query)) {
            return "";
        }
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            params.put(kv[0], kv.length > 1 ? kv[1] : "");
        }
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private String sanitize(String value) {
        return (value + "").replace("\r", "_").replace("\n", "_");
    }
}
