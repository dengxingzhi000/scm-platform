package com.scmcloud.gateway.client;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import org.apache.commons.codec.digest.DigestUtils;

import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Client-side API signature generator.
 * <p>
 * Produces HMAC-SHA256 signatures using the same canonical format as
 * {@code AbstractHmacSignatureAlgorithm} on the server side, ensuring
 * clients can generate headers that pass gateway verification.
 *
 * @author Deng
 * @since 2025/11/11
 */
public final class SignatureClient {

    private static final String DEFAULT_VERSION = "HMAC-SHA256-V2";

    private SignatureClient() {
    }

    /**
     * Generate signed headers for an API request.
     *
     * @param appId     application identifier
     * @param secretKey HMAC signing key
     * @param fullPath  full request path including query string (e.g. {@code /api/orders?page=1&size=10})
     * @param body      request body (may be {@code null} or empty)
     * @return immutable map of signing headers to attach to the HTTP request
     */
    public static Map<String, String> generateHeaders(String appId, String secretKey,
                                                      String fullPath, String body) {
        return generateHeaders(appId, secretKey, fullPath, body, DEFAULT_VERSION);
    }

    /**
     * Generate signed headers for an API request with a specific algorithm version.
     */
    public static Map<String, String> generateHeaders(String appId, String secretKey,
                                                      String fullPath, String body,
                                                      String version) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");

        URI uri = URI.create(fullPath);
        String path = uri.getRawPath();
        String query = canonicalizeQuery(uri.getRawQuery());

        byte[] bodyBytes = body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0];
        String bodyHash = DigestUtils.sha256Hex(bodyBytes);

        String canonicalPayload = buildCanonicalPayload(timestamp, nonce, appId, path, query, bodyHash);
        String signature = SecureUtil.hmac(HmacAlgorithm.HmacSHA256, secretKey)
                .digestHex(canonicalPayload);

        return Map.of(
                "X-Timestamp", timestamp,
                "X-Nonce", nonce,
                "X-Signature", signature,
                "X-App-Id", appId,
                "X-Sign-Version", version
        );
    }

    /**
     * Build the canonical payload string matching the server-side format.
     */
    private static String buildCanonicalPayload(String timestamp, String nonce, String appId,
                                                String path, String query, String bodyHash) {
        return String.format("""
                        ts=%s
                        nonce=%s
                        appId=%s
                        path=%s
                        query=%s
                        bodyHash=%s
                        """, timestamp, nonce, appId, path, query, bodyHash);
    }

    private static String canonicalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return "";
        }

        return Stream.of(rawQuery.split("&"))
                .map(pair -> {
                    int eq = pair.indexOf('=');
                    if (eq < 0) {
                        return Map.entry(urlDecode(pair), "");
                    }
                    return Map.entry(urlDecode(pair.substring(0, eq)),
                            urlDecode(pair.substring(eq + 1)));
                })
                .sorted(Comparator.comparing(Map.Entry<String, String>::getKey)
                        .thenComparing(Map.Entry::getValue))
                .map(e -> UriUtils.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "=" + UriUtils.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static String urlDecode(String value) {
        try {
            return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
