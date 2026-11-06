package com.cognora.lms.logistics.support.webhook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 批次 3：Webhook 验签用例（成功 / 签名错误 / 时间窗超限 / nonce 重放）。
 */
class WebhookSignatureVerifierTest {

    private static final String SECRET = "unit-test-secret";
    private static final long NOW = 1_800_000_000L;
    private static final String BODY = "{\"waybillNo\":\"WB-1\",\"trackStatus\":\"DELIVERED\"}";

    private WebhookSignatureVerifier verifier() {
        return new WebhookSignatureVerifier(SECRET, 300);
    }

    @Test
    void validSignatureWithinWindowShouldPass() {
        WebhookSignatureVerifier verifier = verifier();
        String ts = String.valueOf(NOW);
        String nonce = "nonce-1";
        String signature = verifier.computeSignature(ts, nonce, BODY);

        assertDoesNotThrow(() -> verifier.verifyAndConsumeAt(ts, nonce, signature, BODY, NOW));
    }

    @Test
    void sha256PrefixShouldBeAccepted() {
        WebhookSignatureVerifier verifier = verifier();
        String ts = String.valueOf(NOW);
        String nonce = "nonce-prefix";
        String signature = "sha256=" + verifier.computeSignature(ts, nonce, BODY);

        assertDoesNotThrow(() -> verifier.verifyAndConsumeAt(ts, nonce, signature, BODY, NOW));
    }

    @Test
    void tamperedBodyShouldFail() {
        WebhookSignatureVerifier verifier = verifier();
        String ts = String.valueOf(NOW);
        String signature = verifier.computeSignature(ts, "nonce-2", BODY);

        assertThrows(WebhookSignatureException.class,
                () -> verifier.verifyAndConsumeAt(ts, "nonce-2", signature, BODY + " ", NOW));
    }

    @Test
    void expiredTimestampShouldFail() {
        WebhookSignatureVerifier verifier = verifier();
        String ts = String.valueOf(NOW - 301);
        String signature = verifier.computeSignature(ts, "nonce-3", BODY);

        assertThrows(WebhookSignatureException.class,
                () -> verifier.verifyAndConsumeAt(ts, "nonce-3", signature, BODY, NOW));
    }

    @Test
    void exactlyAtWindowBoundaryShouldPass() {
        WebhookSignatureVerifier verifier = verifier();
        String ts = String.valueOf(NOW - 300);
        String signature = verifier.computeSignature(ts, "nonce-boundary", BODY);

        assertDoesNotThrow(() -> verifier.verifyAndConsumeAt(ts, "nonce-boundary", signature, BODY, NOW));
    }

    @Test
    void replayedNonceShouldBeRejected() {
        WebhookSignatureVerifier verifier = verifier();
        String ts = String.valueOf(NOW);
        String nonce = "nonce-replay";
        String signature = verifier.computeSignature(ts, nonce, BODY);

        verifier.verifyAndConsumeAt(ts, nonce, signature, BODY, NOW);
        // 窗口内同一 nonce 再次使用视为重放。
        assertThrows(WebhookSignatureException.class,
                () -> verifier.verifyAndConsumeAt(ts, nonce, signature, BODY, NOW));
    }

    @Test
    void missingSecretShouldFailClosed() {
        WebhookSignatureVerifier verifier = new WebhookSignatureVerifier("", 300);
        String ts = String.valueOf(NOW);

        assertThrows(WebhookSignatureException.class,
                () -> verifier.verifyAndConsumeAt(ts, "n", "sig", BODY, NOW));
    }

    @Test
    void missingHeadersShouldFail() {
        WebhookSignatureVerifier verifier = verifier();
        assertThrows(WebhookSignatureException.class,
                () -> verifier.verifyAndConsumeAt(null, "n", "sig", BODY, NOW));
        assertThrows(WebhookSignatureException.class,
                () -> verifier.verifyAndConsumeAt(String.valueOf(NOW), "n", null, BODY, NOW));
    }
}
