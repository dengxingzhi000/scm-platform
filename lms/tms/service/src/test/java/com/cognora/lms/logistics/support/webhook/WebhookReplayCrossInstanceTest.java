package com.cognora.lms.logistics.support.webhook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 跨实例 nonce 防重放语义：两个验签器共享同一存储，第二个实例重复消费必须被拒绝。
 *
 * <p>生产环境该共享存储为 Redis（见 {@link RedisWebhookReplayStore}），
 * 本用例用同一 {@link InMemoryWebhookReplayStore} 实例模拟共享存储语义。</p>
 */
class WebhookReplayCrossInstanceTest {

    private static final String SECRET = "unit-test-secret";
    private static final long NOW = 1_800_000_000L;
    private static final String BODY = "{\"waybillNo\":\"WB-1\",\"trackStatus\":\"DELIVERED\"}";

    @Test
    void nonceConsumedByOneInstanceShouldBeRejectedByAnother() {
        WebhookReplayStore sharedStore = new InMemoryWebhookReplayStore();
        WebhookSignatureVerifier nodeA = new WebhookSignatureVerifier(SECRET, 300, sharedStore);
        WebhookSignatureVerifier nodeB = new WebhookSignatureVerifier(SECRET, 300, sharedStore);

        String ts = String.valueOf(NOW);
        String nonce = "nonce-cross-instance";
        String signature = nodeA.computeSignature(ts, nonce, BODY);

        assertDoesNotThrow(() -> nodeA.verifyAndConsumeAt(ts, nonce, signature, BODY, NOW));
        // 同一 nonce 落到另一个实例（共享存储）同样视为重放。
        assertThrows(WebhookSignatureException.class,
                () -> nodeB.verifyAndConsumeAt(ts, nonce, signature, BODY, NOW));
    }

    @Test
    void distinctNoncesShouldBothPassAcrossInstances() {
        WebhookReplayStore sharedStore = new InMemoryWebhookReplayStore();
        WebhookSignatureVerifier nodeA = new WebhookSignatureVerifier(SECRET, 300, sharedStore);
        WebhookSignatureVerifier nodeB = new WebhookSignatureVerifier(SECRET, 300, sharedStore);

        String ts = String.valueOf(NOW);
        String signatureA = nodeA.computeSignature(ts, "nonce-a", BODY);
        String signatureB = nodeB.computeSignature(ts, "nonce-b", BODY);

        assertDoesNotThrow(() -> nodeA.verifyAndConsumeAt(ts, "nonce-a", signatureA, BODY, NOW));
        assertDoesNotThrow(() -> nodeB.verifyAndConsumeAt(ts, "nonce-b", signatureB, BODY, NOW));
    }
}
