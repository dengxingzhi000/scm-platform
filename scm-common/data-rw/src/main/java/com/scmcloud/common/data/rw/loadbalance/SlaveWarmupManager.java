package com.scmcloud.common.data.rw.loadbalance;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slave warmup manager.
 * <p>
 * Newly online slaves need warmup to avoid sudden traffic spike exhausting connection pool.
 * During warmup, slave weight gradually increases to configured value.
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class SlaveWarmupManager {

    private static final int DEFAULT_WARMUP_SECONDS = 60;

    private final Map<String, Instant> slaveOnlineTime = new ConcurrentHashMap<>();
    private final int warmupSeconds;

    public SlaveWarmupManager() {
        this(DEFAULT_WARMUP_SECONDS);
    }

    public SlaveWarmupManager(int warmupSeconds) {
        this.warmupSeconds = warmupSeconds;
    }

    /**
     * Mark slave as online and start warmup.
     *
     * @param slaveName slave name
     */
    public void markOnline(String slaveName) {
        slaveOnlineTime.put(slaveName, Instant.now());
        log.info("[Warmup] Slave [{}] marked as online, warmup started ({}s)", slaveName, warmupSeconds);
    }

    /**
     * Mark slave as offline and clear warmup state.
     *
     * @param slaveName slave name
     */
    public void markOffline(String slaveName) {
        slaveOnlineTime.remove(slaveName);
        log.info("[Warmup] Slave [{}] marked as offline, warmup state cleared", slaveName);
    }

    /**
     * Get effective weight considering warmup.
     *
     * @param slaveName      slave name
     * @param originalWeight original weight
     * @return effective weight
     */
    public int getEffectiveWeight(String slaveName, int originalWeight) {
        Instant onlineTime = slaveOnlineTime.get(slaveName);
        if (onlineTime == null) {
            return originalWeight;
        }

        long elapsedSeconds = Instant.now().getEpochSecond() - onlineTime.getEpochSecond();
        if (elapsedSeconds >= warmupSeconds) {
            return originalWeight;
        }

        // Linear weight increase
        double ratio = (double) elapsedSeconds / warmupSeconds;
        int effectiveWeight = (int) (originalWeight * ratio);

        return Math.max(1, effectiveWeight);
    }

    /**
     * Check if slave is warming up.
     *
     * @param slaveName slave name
     * @return true if warming up
     */
    public boolean isWarmingUp(String slaveName) {
        Instant onlineTime = slaveOnlineTime.get(slaveName);
        if (onlineTime == null) {
            return false;
        }

        long elapsedSeconds = Instant.now().getEpochSecond() - onlineTime.getEpochSecond();
        return elapsedSeconds < warmupSeconds;
    }

    /**
     * Get warmup progress.
     *
     * @param slaveName slave name
     * @return progress percentage (0-100)
     */
    public int getWarmupProgress(String slaveName) {
        Instant onlineTime = slaveOnlineTime.get(slaveName);
        if (onlineTime == null) {
            return 100;
        }

        long elapsedSeconds = Instant.now().getEpochSecond() - onlineTime.getEpochSecond();
        if (elapsedSeconds >= warmupSeconds) {
            return 100;
        }

        return (int) ((elapsedSeconds * 100) / warmupSeconds);
    }
}
