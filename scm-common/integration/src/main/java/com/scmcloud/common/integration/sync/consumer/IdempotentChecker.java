package com.scmcloud.common.integration.sync.consumer;

import com.scmcloud.common.integration.sync.config.DataSyncProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 骞傜瓑鎬ф鏌ュ櫒
 * <p>
 * 鍩轰簬 Redis 瀹炵幇娑堟伅鍘婚噸锛岄槻姝㈤噸澶嶆秷锟?
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@RequiredArgsConstructor
public class IdempotentChecker {
    private final StringRedisTemplate redisTemplate;
    private final DataSyncProperties.IdempotentConfig config;

    /**
     * 灏濊瘯鑾峰彇澶勭悊锟?
     * <p>
     * 浣跨敤 Redis SETNX 瀹炵幇鍒嗗竷寮忛攣璇箟
     *
     * @param eventId 浜嬩欢 ID
     * @return true 濡傛灉鑾峰彇鎴愬姛锛堥娆″鐞嗭級锛宖alse 濡傛灉宸插鐞嗚繃
     */
    public boolean tryAcquire(String eventId) {
        if (!config.isEnabled()) {
            return true;
        }

        String key = config.getKeyPrefix() + eventId;
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "processing", Duration.ofSeconds(config.getExpireSeconds()));

        if (Boolean.TRUE.equals(success)) {
            log.debug("[Idempotent] Acquired processing lock: eventId={}", eventId);
            return true;
        } else {
            log.debug("[Idempotent] Duplicate event detected: eventId={}", eventId);
            return false;
        }
    }

    /**
     * 鏍囪澶勭悊瀹屾垚
     *
     * @param eventId 浜嬩欢 ID
     */
    public void markCompleted(String eventId) {
        if (!config.isEnabled()) {
            return;
        }

        String key = config.getKeyPrefix() + eventId;
        redisTemplate.opsForValue().set(key, "completed", Duration.ofSeconds(config.getExpireSeconds()));
        log.debug("[Idempotent] Marked as completed: eventId={}", eventId);
    }

    /**
     * 鏍囪澶勭悊澶辫触锛堥噴鏀鹃攣锛屽厑璁搁噸璇曪級
     *
     * @param eventId 浜嬩欢 ID
     */
    public void markFailed(String eventId) {
        if (!config.isEnabled()) {
            return;
        }

        String key = config.getKeyPrefix() + eventId;
        redisTemplate.delete(key);
        log.debug("[Idempotent] Marked as failed, lock released: eventId={}", eventId);
    }

    /**
     * 妫€鏌ユ槸鍚﹀凡澶勭悊
     *
     * @param eventId 浜嬩欢 ID
     * @return true 濡傛灉宸插锟?
     */
    public boolean isProcessed(String eventId) {
        if (!config.isEnabled()) {
            return false;
        }

        String key = config.getKeyPrefix() + eventId;
        String status = redisTemplate.opsForValue().get(key);
        return "completed".equals(status);
    }
}
