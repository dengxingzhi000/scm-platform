package com.cognora.lms.logistics.support.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 承运商 Webhook 验签（批次 3）。
 *
 * <p>签名方案：{@code HMAC-SHA256(secret, timestamp + "\n" + nonce + "\n" + rawBody)}，
 * 请求头：{@code X-Lms-Timestamp}（Unix 秒）、{@code X-Lms-Nonce}、{@code X-Lms-Signature}（hex，可带 sha256= 前缀）。</p>
 *
 * <p>防重放为「时间窗 + nonce 一次性消费 + event_id 幂等」三层组合：</p>
 * <ol>
 *   <li>时间窗：时间戳与服务器时间偏差超过阈值直接拒绝（默认 300 秒）；</li>
 *   <li>nonce：窗口内同一 nonce 只允许消费一次，拦截窗口内重放；</li>
 *   <li>event_id：验签通过后仍由轨迹服务按 event_id 幂等，拦截跨实例/窗口外的重复推送。</li>
 * </ol>
 *
 * <p>密钥仅来自环境变量/配置，禁止明文落库与日志输出；密钥缺失时 fail-closed。</p>
 */
@Component
public class WebhookSignatureVerifier {

    public static final long DEFAULT_WINDOW_SECONDS = 300;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final String secret;
    private final long windowSeconds;
    /** nonce -> 首次消费时间戳（秒），按窗口惰性清理。 */
    private final Map<String, Long> consumedNonces = new ConcurrentHashMap<>();

    public WebhookSignatureVerifier(
            @Value("${lms.webhook.hmac-secret:}") String secret,
            @Value("${lms.webhook.time-window-seconds:300}") long windowSeconds) {
        this.secret = secret;
        this.windowSeconds = windowSeconds > 0 ? windowSeconds : DEFAULT_WINDOW_SECONDS;
    }

    /**
     * 校验并消费一次回调凭证。任一校验失败抛出 {@link WebhookSignatureException}，调用方不得落库。
     */
    public void verifyAndConsume(String timestamp, String nonce, String signature, String rawBody) {
        verifyAndConsumeAt(timestamp, nonce, signature, rawBody, System.currentTimeMillis() / 1000);
    }

    /**
     * 可注入当前时间戳的校验入口，便于覆盖时间窗边界（包内可见）。
     */
    void verifyAndConsumeAt(String timestamp, String nonce, String signature, String rawBody, long nowSeconds) {
        if (secret == null || secret.isBlank()) {
            throw new WebhookSignatureException("Webhook 密钥未配置，拒绝回调");
        }
        if (timestamp == null || timestamp.isBlank() || nonce == null || nonce.isBlank()) {
            throw new WebhookSignatureException("缺少回调签名时间戳或 nonce");
        }
        if (signature == null || signature.isBlank()) {
            throw new WebhookSignatureException("缺少回调签名");
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            throw new WebhookSignatureException("回调时间戳格式非法");
        }
        if (Math.abs(nowSeconds - ts) > windowSeconds) {
            throw new WebhookSignatureException("回调时间戳超出允许时间窗");
        }

        String expected = computeSignature(timestamp.trim(), nonce.trim(), rawBody == null ? "" : rawBody);
        String actual = signature.trim();
        if (actual.regionMatches(true, 0, SIGNATURE_PREFIX, 0, SIGNATURE_PREFIX.length())) {
            actual = actual.substring(SIGNATURE_PREFIX.length());
        }
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
            throw new WebhookSignatureException("回调签名校验失败");
        }

        pruneExpired(nowSeconds);
        // putIfAbsent 保证同一 nonce 并发回调只有一个能通过。
        if (consumedNonces.putIfAbsent(nonce.trim(), ts) != null) {
            throw new WebhookSignatureException("回调 nonce 已被使用（疑似重放）");
        }
    }

    /**
     * 计算签名，供测试与联调自检；不包含任何密钥输出。
     */
    public String computeSignature(String timestamp, String nonce, String rawBody) {
        if (secret == null || secret.isBlank()) {
            throw new WebhookSignatureException("Webhook 密钥未配置");
        }
        String payload = timestamp + "\n" + nonce + "\n" + (rawBody == null ? "" : rawBody);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // 不输出底层异常细节，避免泄露密钥材料。
            throw new WebhookSignatureException("回调签名计算失败");
        }
    }

    private void pruneExpired(long nowSeconds) {
        consumedNonces.entrySet().removeIf(entry -> Math.abs(nowSeconds - entry.getValue()) > windowSeconds);
    }
}
