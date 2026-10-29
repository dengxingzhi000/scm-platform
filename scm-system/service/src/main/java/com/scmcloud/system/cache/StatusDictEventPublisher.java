package com.scmcloud.system.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 状态字典变更事件发布器。
 * 当管理员修改状态字典/流转规则时，发送 Kafka 事件通知所有服务刷新缓存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatusDictEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "status-dict-change";

    /**
     * 通知指定租户+业务类型的缓存需要刷新
     */
    public void publishChange(String tenantId, String bizType) {
        String payload = tenantId + ":" + bizType;
        kafkaTemplate.send(TOPIC, bizType, payload);
        log.info("Published status dict change: {}", (payload + "").replace("\r", "_").replace("\n", "_"));
    }

    /**
     * 通知全量缓存刷新
     */
    public void publishRefreshAll() {
        kafkaTemplate.send(TOPIC, "ALL", "ALL");
        log.info("Published status dict refresh ALL");
    }
}
