package com.scmcloud.product.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 閰嶇疆锟?
 *
 * <p>閰嶇疆 Kafka Consumer 鐢ㄤ簬鎺ユ敹 Debezium 鍙樻洿浜嬩欢
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:product-sync-group}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${spring.kafka.consumer.enable-auto-commit:false}")
    private Boolean enableAutoCommit;

    /**
     * Kafka Consumer 宸ュ巶
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Kafka 闆嗙兢鍦板潃
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // 娑堣垂鑰呯粍 ID
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // 鑷姩鎻愪氦鍋忕Щ閲忥紙璁剧疆锟絝alse锛屾墜鍔ㄦ彁浜わ級
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);

        // 鍋忕Щ閲忛噸缃瓥鐣ワ紙earliest: 浠庢渶鏃╃殑娑堟伅寮€濮嬫秷璐癸級
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

        // Key 鍙嶅簭鍒楀寲锟?
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Value 鍙嶅簭鍒楀寲锟?
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // 鍗曟鎷夊彇鏈€澶ф秷鎭暟
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        // 浼氳瘽瓒呮椂鏃堕棿锟? 绉掞級
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);

        // 蹇冭烦闂撮殧锟?绉掞級
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Kafka Listener 瀹瑰櫒宸ュ巶
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        // 璁剧疆骞跺彂绾у埆锛堢嚎绋嬫暟锟?
        factory.setConcurrency(3);

        // 璁剧疆鎵嬪姩鎻愪氦妯″紡
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // 璁剧疆鎵归噺鐩戝惉锛堝彲閫夛級
        // factory.setBatchListener(true);

        return factory;
    }
}