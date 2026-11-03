package com.scmcloud.system.cache;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 监听状态字典变更事件，刷新本地缓存。
 * 所有服务实例都会收到消息并清除自己的 L1 缓存。
 *
 * <p>事件格式: "tenantId:bizType" 或 "ALL" (全量刷新)
 */
@Slf4j
@Component
public class StatusDictCacheEventListener {

    private final StatusDictCacheManager cacheManager;

    public StatusDictCacheEventListener(StatusDictCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @KafkaListener(topics = "status-dict-change", groupId = "status-dict-cache")
    public void onStatusDictChanged(ConsumerRecord<String, String> record) {
        String payload = record.value();
        log.info("Received status dict change event: {}", payload);

        if ("ALL".equals(payload)) {
            cacheManager.evictAll();
            log.info("All status dict cache evicted");
            return;
        }

        // 格式: "tenantId:bizType"
        String[] parts = payload.split(":");
        if (parts.length == 2) {
            String tenantId = parts[0];
            String bizType = parts[1];
            cacheManager.refresh(tenantId, bizType);
        } else {
            log.warn("Invalid status dict change event format: {}", payload);
        }
    }
}
