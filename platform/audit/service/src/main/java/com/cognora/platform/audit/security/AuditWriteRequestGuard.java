package com.cognora.platform.audit.security;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.cognora.platform.common.rest.signature.InternalSignatureSecretResolver;
import com.cognora.platform.common.security.metrics.SecurityMetrics;
import com.cognora.platform.common.security.util.SecurityErrorResponseWriter;
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
 * 瀹¤鍐欏叆鍏ュ彛鐨勮皟鐢ㄦ柟鏍￠獙銆? *
 * <p>{@code POST /sys-audit-log} 鏄?scm-common 鐨?{@code SysAuditServiceClient} 浣跨敤鐨勬湇鍔￠棿鍐欏叆閫氶亾锛? * 璋冪敤鏂逛細鎼哄甫 {@code RestClientRequestSignatureInterceptor} 鐢熸垚鐨?HMAC 澶淬€傛湰杩囨护鍣ㄥ彧鏀捐甯﹀悎娉? * 绛惧悕鐨勫唴閮ㄨ皟鐢紝闃叉浠绘剰鐧诲綍鐢ㄦ埛浼€?鍒犻櫎瀹¤璁板綍銆傛牎楠岀畻娉曚笌鎷︽埅鍣ㄤ繚鎸佷竴鑷达細
 * <pre>signature = HMAC-SHA256(secretKey, timestamp + nonce + appId + uri + sortedParams)</pre>
 *
 * <p>瀵嗛挜鐢?{@link InternalSignatureSecretResolver} 瑙ｆ瀽锛岀鍚嶆柟涓庢湰鏍￠獙鏂硅鍚屼竴涓€硷紱缂哄け鏃? * {@link AuditWriteSecurityValidator} 浼氳鏈嶅姟鐩存帴鍚姩澶辫触锛岃€屼笉鏄€€鍥炲埌浠撳簱閲岀殑甯搁噺銆? *
 * <p>璺緞鍖归厤鎸?Spring MVC 鐨勮鍒欏綊涓€鍖栵紙瑙ｇ爜銆佸幓鎺夌煩闃靛弬鏁般€佸幓鎺夋湯灏炬枩鏉狅級锛? * {@code /sys-audit-log;x=1}銆亄@code /%73ys-audit-log} 杩欑被鍙樹綋涓嶄細缁曡繃绛惧悕鏍￠獙銆? *
 * <p>鏈牎楠?nonce 閲嶆斁锛歴cm-audit 涓嶄緷璧?Redis銆傛椂闂存埑绐楀彛闄愬埗涓?5 鍒嗛挓銆? */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditWriteRequestGuard extends OncePerRequestFilter {

    static final String WRITE_PATH = "/sys-audit-log";

    /** 琚嫆缁濈殑鍐欏叆娆℃暟锛岃繍缁村湪 scm-audit 涓婅瀵熻鎸囨爣銆?*/
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

        // 绛惧悕缁戝畾鐨勬槸鏀跺埌鐨勫師濮嬭姹傜洰鏍囷細鍚屼竴浠界鍚嶆棤娉曡閲嶆斁鍒板彟涓€绉嶅啓娉曚笂銆?        String signContent = timestamp + nonce + appId + request.getRequestURI()
                + sortAndConcatParams(request.getQueryString());
        String expected = SecureUtil.hmac(HmacAlgorithm.HmacSHA256, secretKey.getBytes(StandardCharsets.UTF_8))
                .digestHex(signContent);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 鍒ゆ柇璇锋眰鏄惁鎸囧悜瀹¤鍐欏叆鍏ュ彛銆?     *
     * <p>Spring MVC 鐢ㄣ€岃В鐮佸悗銆佸幓鎺夌煩闃靛弬鏁般€嶇殑璺緞鍋氬尮閰嶏紝鎵€浠?{@code /sys-audit-log;x=1}銆?     * {@code /%73ys-audit-log} 涓?{@code /sys-audit-log/} 閮戒細钀藉埌鍚屼竴涓?Controller銆傜洿鎺ユ瘮杈?     * {@code getRequestURI()} 浼氳杩欎簺鍙樹綋缁曡繃绛惧悕鏍￠獙銆?     */
    private boolean isAuditWritePath(String rawUri) {
        String normalized = normalizePath(rawUri);
        // 鏃犳硶瑙ｇ爜鐨勮矾寰勬寜鍐欏叆灏濊瘯澶勭悊锛坒ail closed锛夛細瀹佸彲璇嫆锛屼篃涓嶈兘鏀捐繃銆?        return normalized == null || WRITE_PATH.equals(normalized);
    }

    /**
     * 鎸?Spring 鐨勫尮閰嶈鍒欏綊涓€鍖栬矾寰勶細瑙ｇ爜銆佸幓鎺夌煩闃靛弬鏁般€佸幓鎺夋湯灏炬枩鏉犮€?     *
     * @return 褰掍竴鍖栧悗鐨勮矾寰勶紱璺緞鏃犳硶瑙ｇ爜鏃惰繑鍥?{@code null}
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
