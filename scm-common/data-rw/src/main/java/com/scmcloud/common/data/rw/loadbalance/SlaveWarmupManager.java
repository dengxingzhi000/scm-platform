package com.scmcloud.common.data.rw.loadbalance;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Slave warmup manager.
 * <p>
 * Two responsibilities:
 * <ol>
 *   <li><b>Weight ramp</b> — a newly online slave starts with a fraction of its
 *       configured weight and reaches it after {@code warmupSeconds}. Only weighted
 *       load balancers ({@code WEIGHTED_ROUND_ROBIN}/{@code WEIGHTED_RANDOM}) use it;
 *       round-robin and least-connections ignore the weight.</li>
 *   <li><b>Connection pre-warm</b> — after the application is ready, opens one
 *       connection per slave asynchronously, records the elapsed time and success,
 *       logs it, and marks a failed slave offline. It never blocks startup.</li>
 * </ol>
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class SlaveWarmupManager {

    private static final int DEFAULT_WARMUP_SECONDS = 60;

    private final Map<String, Instant> slaveOnlineTime = new ConcurrentHashMap<>();
    private final Map<String, WarmupResult> warmupResults = new ConcurrentHashMap<>();
    private final int warmupSeconds;
    private final ExecutorService warmupExecutor;

    public SlaveWarmupManager() {
        this(DEFAULT_WARMUP_SECONDS);
    }

    public SlaveWarmupManager(int warmupSeconds) {
        this.warmupSeconds = warmupSeconds;
        this.warmupExecutor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "rw-slave-warmup");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Warm up every configured slave without blocking the caller.
     */
    public void warmupAll(Map<String, ? extends Map<String, DataSource>> slavesByGroup) {
        slavesByGroup.forEach((group, slaves) ->
                slaves.forEach((slaveName, dataSource) -> warmup(group, slaveName, dataSource)));
    }

    /**
     * Asynchronously open one connection on the slave, record its latency and mark the
     * slave offline when it fails. The returned future completes on the warmup thread.
     */
    public CompletableFuture<WarmupResult> warmup(String groupName, String slaveName, DataSource dataSource) {
        markOnline(slaveName);
        String key = groupName + "." + slaveName;
        long startedAt = System.nanoTime();
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                boolean valid = connection.isValid(3);
                long elapsedMs = elapsedMillis(startedAt);
                WarmupResult result = new WarmupResult(valid, elapsedMs,
                        valid ? null : "connection.isValid() returned false");
                warmupResults.put(key, result);
                if (valid) {
                    log.info("[Warmup] Slave [{}] prewarmed in {}ms", key, elapsedMs);
                } else {
                    markOffline(slaveName);
                    log.warn("[Warmup] Slave [{}] prewarmed in {}ms but isValid=false", key, elapsedMs);
                }
                return result;
            } catch (Exception e) {
                long elapsedMs = elapsedMillis(startedAt);
                markOffline(slaveName);
                WarmupResult result = new WarmupResult(false, elapsedMs, e.getMessage());
                warmupResults.put(key, result);
                log.warn("[Warmup] Slave [{}] prewarm failed after {}ms: {}", key, elapsedMs, e.getMessage());
                return result;
            }
        }, warmupExecutor);
    }

    public Map<String, WarmupResult> getWarmupResults() {
        return Map.copyOf(warmupResults);
    }

    public Map<String, Object> getSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        warmupResults.forEach((key, result) -> snapshot.put(key, Map.of(
                "success", result.success(),
                "elapsedMs", result.elapsedMs(),
                "error", result.error() == null ? "" : result.error())));
        return snapshot;
    }

    public void shutdown() {
        warmupExecutor.shutdownNow();
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

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    /**
     * Result of one connection pre-warm.
     */
    public record WarmupResult(boolean success, long elapsedMs, String error) {
    }
}
