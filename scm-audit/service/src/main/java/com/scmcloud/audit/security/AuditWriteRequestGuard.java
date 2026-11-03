package com.scmcloud.audit.security;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.scmcloud.common.rest.signature.InternalSignatureSecretResolver;
import com.scmcloud.common.security.metrics.SecurityMetrics;
import com.scmcloud.common.security.util.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;

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
 * <p>密钥由 {@link InternalSignatureSecretResolver} 解析，签名方与本校验方读同一个值；缺失时
 * {@link AuditWriteSecurityValidator} 会让服务直接启动失败，而不是退回到仓库里的常量。
 *
 * <p>路径匹配按 Spring MVC 的规则归一化（解码、去掉矩阵参数、去掉末尾斜杠），
 * {@code /sys-audit-log;x=1}、{@code /%73ys-audit-log} 这类变体不会绕过签名校验。
 *
 * <p>未校验 nonce 重放：scm-audit 不依赖 Redis。时间戳窗口限制为 5 分钟。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditWriteRequestGuard extends OncePerRequestFilter {

    static final String WRITE_PATH = "/sys-audit-log";

    /** 被拒绝的写入次数，运维在 scm-audit 上观察该指标。 */
    static final String METRIC_REJECTED = "security.audit.write.rejected";

    private static final long MAX_CLOCK_SKEW_MILLIS = 5 * 60 * 1000L;

    private final InternalSignatureSecretResolver secretResolver;
    private final SecurityMetrics securityMetrics;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return true;
        }
        return !isAuditWritePath(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!hasValidSignature(request)) {
            reject(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    boolean hasValidSignature(HttpServletRequest request) {
        String secretKey = secretResolver.resolveSecret();
        if (!StringUtils.hasText(secretKey)) {
            return false;
        }
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

        // 签名绑定的是收到的原始请求目标：同一份签名无法被重放到另一种写法上。
        String signContent = timestamp + nonce + appId + request.getRequestURI()
                + sortAndConcatParams(request.getQueryString());
        String expected = SecureUtil.hmac(HmacAlgorithm.HmacSHA256, secretKey.getBytes(StandardCharsets.UTF_8))
                .digestHex(signContent);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 判断请求是否指向审计写入入口。
     *
     * <p>Spring MVC 用「解码后、去掉矩阵参数」的路径做匹配，所以 {@code /sys-audit-log;x=1}、
     * {@code /%73ys-audit-log} 与 {@code /sys-audit-log/} 都会落到同一个 Controller。直接比较
     * {@code getRequestURI()} 会让这些变体绕过签名校验。
     */
    private boolean isAuditWritePath(String rawUri) {
        String normalized = normalizePath(rawUri);
        // 无法解码的路径按写入尝试处理（fail closed）：宁可误拒，也不能放过。
        return normalized == null || WRITE_PATH.equals(normalized);
    }

    /**
     * 按 Spring 的匹配规则归一化路径：解码、去掉矩阵参数、去掉末尾斜杠。
     *
     * @return 归一化后的路径；路径无法解码时返回 {@code null}
     */
    static String normalizePath(String rawUri) {
        if (rawUri == null) {
            return null;
        }
        String[] segments = rawUri.split("/", -1);
        StringBuilder normalized = new StringBuilder(rawUri.length());
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                normalized.append('/');
            }
            String segment = segments[i];
            int matrixStart = segment.indexOf(';');
            if (matrixStart >= 0) {
                segment = segment.substring(0, matrixStart);
            }
            try {
                segment = UriUtils.decode(segment, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return null;
            }
            normalized.append(segment);
        }
        String path = normalized.toString();
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        securityMetrics.increment(METRIC_REJECTED);
        log.error("Rejected audit write: appId={}, remote={}, uri={}, traceId={}",
                sanitize(request.getHeader("X-App-Id")), sanitize(request.getRemoteAddr()),
                sanitize(request.getRequestURI()), sanitize(request.getHeader("X-Request-ID")));
        SecurityErrorResponseWriter.write(request, response, HttpServletResponse.SC_FORBIDDEN,
                "ACCESS_DENIED", "Audit writes are restricted to internal services");
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
