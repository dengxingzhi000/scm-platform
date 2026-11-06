package com.cognora.lms.logistics.support.webhook;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Webhook 防重放存储装配：lms-tms 已接入 Redis，生产统一走共享存储。
 *
 * <p>单元测试直接使用 {@link InMemoryWebhookReplayStore}，不依赖 Spring 上下文。</p>
 */
@Configuration
public class WebhookReplayStoreConfig {

    @Bean
    public WebhookReplayStore webhookReplayStore(StringRedisTemplate redisTemplate) {
        return new RedisWebhookReplayStore(redisTemplate);
    }
}
