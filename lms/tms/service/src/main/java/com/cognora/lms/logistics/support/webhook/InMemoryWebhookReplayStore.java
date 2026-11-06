package com.cognora.lms.logistics.support.webhook;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内 nonce 防重放存储：仅适用于单实例部署与单元测试。
 *
 * <p>按时间窗惰性清理，避免长期运行导致 Map 无界增长。</p>
 */
public class InMemoryWebhookReplayStore implements WebhookReplayStore {

    /** nonce -> 首次消费时的请求时间戳（秒）。 */
    private final Map<String, Long> consumedNonces = new ConcurrentHashMap<>();

    @Override
    public boolean consumeIfAbsent(String nonce, long nowSeconds, long windowSeconds) {
        pruneExpired(nowSeconds, windowSeconds);
        // putIfAbsent 保证同一 nonce 并发回调只有一个能通过。
        return consumedNonces.putIfAbsent(nonce, nowSeconds) == null;
    }

    private void pruneExpired(long nowSeconds, long windowSeconds) {
        consumedNonces.entrySet().removeIf(entry -> Math.abs(nowSeconds - entry.getValue()) > windowSeconds);
    }
}
