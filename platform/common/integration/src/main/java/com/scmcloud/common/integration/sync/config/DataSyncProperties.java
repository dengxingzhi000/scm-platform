package com.scmcloud.common.integration.sync.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 鏁版嵁鍚屾閰嶇疆灞烇拷
 *
 * @author Deng
 * @since 2025-12-16
 */
@Data
@ConfigurationProperties(prefix = "datasync")
public class DataSyncProperties {
    /**
     * 鏄惁鍚敤鏁版嵁鍚屾
     */
    private boolean enabled = true;

    /**
     * 褰撳墠鏈嶅姟鍚嶇О
     */
    private String serviceName = "unknown";

    /**
     * Kafka 涓婚鍓嶇紑
     */
    private String topicPrefix = "datasync";

    /**
     * 姝讳俊闃熷垪涓婚
     */
    private String deadLetterTopic = "datasync.dlq";

    /**
     * 鍙戝竷瓒呮椂鏃堕棿锛堟绉掞級
     */
    private long publishTimeoutMs = 5000;

    /**
     * 娑堣垂鑰呴厤锟?
     */
    private ConsumerConfig consumer = new ConsumerConfig();

    /**
     * 閲嶈瘯閰嶇疆
     */
    private RetryConfig retry = new RetryConfig();

    /**
     * 骞傜瓑閰嶇疆
     */
    private IdempotentConfig idempotent = new IdempotentConfig();

    /**
     * 瀵硅处閰嶇疆
     */
    private ReconciliationConfig reconciliation = new ReconciliationConfig();

    /**
     * 澶勭悊鍣ㄦ槧灏勶細aggregateType -> handlerBeanName
     */
    private Map<String, String> handlers = new HashMap<>();

    @Data
    public static class ConsumerConfig {
        /**
         * 娑堣垂鑰呯粍 ID
         */
        private String groupId = "datasync-consumer";

        /**
         * 骞跺彂娑堣垂鑰呮暟锟?
         */
        private int concurrency = 3;

        /**
         * 鎵归噺鎷夊彇澶у皬
         */
        private int batchSize = 100;

        /**
         * 娑堣垂瓒呮椂鏃堕棿
         */
        private Duration pollTimeout = Duration.ofSeconds(5);
    }

    @Data
    public static class RetryConfig {
        /**
         * 鏄惁鍚敤閲嶈瘯
         */
        private boolean enabled = true;

        /**
         * 鏈€澶ч噸璇曟锟?
         */
        private int maxAttempts = 3;

        /**
         * 鍒濆閫€閬块棿闅旓紙姣锟?
         */
        private long initialIntervalMs = 1000;

        /**
         * 鏈€澶ч€€閬块棿闅旓紙姣锟?
         */
        private long maxIntervalMs = 30000;

        /**
         * 閫€閬夸箻锟?
         */
        private double multiplier = 2.0;
    }

    @Data
    public static class IdempotentConfig {
        /**
         * 鏄惁鍚敤骞傜瓑
         */
        private boolean enabled = true;

        /**
         * 骞傜瓑 key 杩囨湡鏃堕棿锛堢锟?
         */
        private long expireSeconds = 86400; // 24 hours

        /**
         * Redis key 鍓嶇紑
         */
        private String keyPrefix = "datasync:idempotent:";
    }

    @Data
    public static class ReconciliationConfig {
        /**
         * 鏄惁鍚敤瀵硅处
         */
        private boolean enabled = false;

        /**
         * 瀵硅处 cron 琛ㄨ揪锟?
         */
        private String cron = "0 0 3 * * ?"; // 姣忓ぉ鍑屾櫒 3 锟?

        /**
         * 瀵硅处鎵规澶у皬
         */
        private int batchSize = 1000;

        /**
         * 鏄惁鑷姩淇
         */
        private boolean autoFix = false;
    }
}
