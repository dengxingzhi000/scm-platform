package com.scmcloud.inventory.lock;

import com.scmcloud.common.redis.script.RedisLuaScriptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Redis 分布式锁
 *
 * <p>基于 Redis 实现的分布式锁，使用 UUID 标识客户端，使用 Lua 脚本保证原子性
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 * @version 1.1
 * @apiNote 1.1 消除 busy-wait，修复 release() 竞态，移除 emoji 日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLock {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_PREFIX = "lock:inventory:";

    private static final RedisScript<Long> ACQUIRE_SCRIPT =
            RedisLuaScriptLoader.load("lua/lock/acquire.lua", Long.class);
    private static final RedisScript<Long> UNLOCK_SCRIPT =
            RedisLuaScriptLoader.load("lua/lock/unlock.lua", Long.class);

    /**
     * 尝试获取锁（不等待）
     *
     * @param key 锁的键
     * @param expireTime 锁的过期时间
     * @param timeUnit 时间单位
     * @return 锁句柄（如果获取成功），null（如果获取失败）
     */
    public LockHandle tryLock(String key, long expireTime, TimeUnit timeUnit) {
        String lockKey = LOCK_PREFIX + key;
        String clientId = UUID.randomUUID().toString();
        long ttlMillis = timeUnit.toMillis(expireTime);

        Long result = redisTemplate.execute(
                ACQUIRE_SCRIPT,
                Collections.singletonList(lockKey),
                clientId,
                String.valueOf(ttlMillis)
        );

        if (result != null && result == 1L) {
            log.debug("获取分布式锁成功: key={}, clientId={}, expireTime={} {}", key, clientId, expireTime, timeUnit);
            return new LockHandle(lockKey, clientId, this);
        }

        log.debug("获取分布式锁失败（锁已被占用） key={}", key);
        return null;
    }

    /**
     * 获取锁（等待并重试）
     *
     * @param key 锁的键
     * @param expireTime 锁的过期时间
     * @param timeUnit 时间单位
     * @param waitTime 最大等待时间
     * @param retryInterval 重试间隔（毫秒）
     * @return 锁句柄（如果获取成功），null（如果超时）
     */
    public LockHandle lock(String key, long expireTime, TimeUnit timeUnit,
                           long waitTime, long retryInterval) {
        long waitMillis = timeUnit.toMillis(waitTime);
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(waitMillis);
        long delay = Math.max(10, retryInterval);

        while (System.nanoTime() < deadline) {
            LockHandle handle = tryLock(key, expireTime, timeUnit);
            if (handle != null) {
                return handle;
            }
            LockSupport.parkNanos(delay * 1_000_000L);
            if (Thread.interrupted()) {
                log.warn("等待锁时被中断 key={}", key);
                return null;
            }
            delay = Math.clamp(delay * 2, 10, 200);
        }

        log.warn("获取分布式锁超时: key={}, waitTime={} ms", key, waitMillis);
        return null;
    }

    /**
     * 释放锁（使用 Lua 脚本保证原子性）
     *
     * @param lockKey 锁的完整键
     * @param clientId 客户端ID
     * @return true-释放成功，false-释放失败（锁不存在或不是当前客户端持有）
     */
    boolean unlock(String lockKey, String clientId) {
        Long result = redisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(lockKey),
                clientId
        );

        if (result != null && result == 1L) {
            log.debug("释放分布式锁成功: key={}, clientId={}", lockKey, clientId);
            return true;
        }

        log.warn("释放分布式锁失败（锁不存在或已过期）: key={}, clientId={}", lockKey, clientId);
        return false;
    }

    /**
     * 锁句柄（用于释放锁）
     */
    public static class LockHandle implements AutoCloseable {
        private final String lockKey;
        private final String clientId;
        private final DistributedLock lock;
        private volatile boolean released = false;

        public LockHandle(String lockKey, String clientId, DistributedLock lock) {
            this.lockKey = lockKey;
            this.clientId = clientId;
            this.lock = lock;
        }

        /**
         * 释放锁
         */
        public synchronized void release() {
            if (!released) {
                lock.unlock(lockKey, clientId);
                released = true;
            }
        }

        /**
         * 支持 try-with-resources 语法
         */
        @Override
        public void close() {
            release();
        }

        public String getLockKey() {
            return lockKey;
        }

        public String getClientId() {
            return clientId;
        }

        public boolean isReleased() {
            return released;
        }
    }
}
