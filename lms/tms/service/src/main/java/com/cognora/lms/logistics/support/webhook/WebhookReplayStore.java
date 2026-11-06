package com.cognora.lms.logistics.support.webhook;

/**
 * Webhook nonce 防重放存储（批次 3 收尾）。
 *
 * <p>多实例部署时必须由共享存储（Redis）实现，保证同一 nonce 在任意实例上只消费一次；
 * 单实例/单元测试可使用 {@link InMemoryWebhookReplayStore}。</p>
 */
public interface WebhookReplayStore {

    /**
     * 尝试一次性消费 nonce。
     *
     * @param nonce        回调 nonce（已去除首尾空白）
     * @param nowSeconds   当前时间戳（Unix 秒），用于进程内实现在窗口外惰性清理
     * @param windowSeconds 允许的时间窗（秒），Redis 实现按 2 倍窗口设置 TTL
     * @return true 表示首次消费成功；false 表示窗口内已被消费（疑似重放）
     */
    boolean consumeIfAbsent(String nonce, long nowSeconds, long windowSeconds);
}
