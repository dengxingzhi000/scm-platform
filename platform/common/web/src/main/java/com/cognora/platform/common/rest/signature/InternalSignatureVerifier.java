package com.cognora.platform.common.rest.signature;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Verifies the HMAC signature of a service-to-service request.
 *
 * <p>The algorithm mirrors {@code RestClientRequestSignatureInterceptor} exactly:
 * <pre>signature = HMAC-SHA256(secretKey, timestamp + nonce + appId + requestURI + sortedParams)</pre>
 *
 * <p>The secret is resolved through {@link InternalSignatureSecretResolver}; a blank secret makes
 * every verification fail closed. Timestamps outside a five minute window are rejected. The nonce
 * is not checked for replay here (no shared Redis), only the clock window.
 */
@Component
@RequiredArgsConstructor
public class InternalSignatureVerifier {

    private static final long MAX_CLOCK_SKEW_MILLIS = 5 * 60 * 1000L;

    private final InternalSignatureSecretResolver secretResolver;

    public boolean hasValidSignature(HttpServletRequest request) {
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

        String signContent = timestamp + nonce + appId + request.getRequestURI()
                + sortAndConcatParams(request.getQueryString());
        String expected = SecureUtil.hmac(HmacAlgorithm.HmacSHA256, secretKey.getBytes(StandardCharsets.UTF_8))
                .digestHex(signContent);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    static String sortAndConcatParams(String query) {
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
}
