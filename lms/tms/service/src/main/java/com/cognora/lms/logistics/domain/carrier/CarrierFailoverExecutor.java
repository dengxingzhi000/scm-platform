package com.cognora.lms.logistics.domain.carrier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 承运商调用重试/熔断/降级执行器（批次 3）。
 *
 * <p>策略：</p>
 * <ul>
 *   <li>重试：同一承运商最多 {@code maxAttempts} 次，指数退避 base×2^n（封顶 maxBackoff）；</li>
 *   <li>熔断：连续失败达到 {@code failureThreshold} 次后打开，冷却期内直接跳过该承运商；</li>
 *   <li>降级：主承运商失败或熔断时，按备用列表顺序尝试；发生切换即标记 degraded。</li>
 * </ul>
 *
 * <p>无状态存储为进程内（单实例），多实例部署需替换为共享缓存/注册中心；真实承运商 SDK 未接入。</p>
 */
public final class CarrierFailoverExecutor {

    public record CallOutcome<T>(boolean success, String carrierId, int attempts, boolean degraded, String error) {
    }

    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private static final class Circuit {
        private int consecutiveFailures;
        private long openUntilMillis;
    }

    private final int failureThreshold;
    private final long cooldownMillis;
    private final int maxAttempts;
    private final long baseBackoffMillis;
    private final long maxBackoffMillis;
    private final Sleeper sleeper;
    private final Map<String, Circuit> circuits = new ConcurrentHashMap<>();

    public CarrierFailoverExecutor() {
        this(3, 60_000L, 2, 100L, 1_000L, Thread::sleep);
    }

    public CarrierFailoverExecutor(int failureThreshold, long cooldownMillis, int maxAttempts,
                                   long baseBackoffMillis, long maxBackoffMillis, Sleeper sleeper) {
        this.failureThreshold = failureThreshold > 0 ? failureThreshold : 3;
        this.cooldownMillis = cooldownMillis > 0 ? cooldownMillis : 60_000L;
        this.maxAttempts = maxAttempts > 0 ? maxAttempts : 1;
        this.baseBackoffMillis = Math.max(baseBackoffMillis, 0);
        this.maxBackoffMillis = Math.max(maxBackoffMillis, baseBackoffMillis);
        this.sleeper = sleeper == null ? Thread::sleep : sleeper;
    }

    /**
     * 依次尝试主承运商与备用承运商。任一成功即返回；全部失败返回 success=false。
     */
    public <T> CallOutcome<T> execute(String primaryCarrierId, List<String> backupCarrierIds,
                                      Function<String, T> call) {
        List<String> candidates = new ArrayList<>();
        if (primaryCarrierId != null && !primaryCarrierId.isBlank()) {
            candidates.add(primaryCarrierId);
        }
        if (backupCarrierIds != null) {
            for (String backup : backupCarrierIds) {
                if (backup != null && !backup.isBlank() && !candidates.contains(backup)) {
                    candidates.add(backup);
                }
            }
        }

        String lastError = null;
        int totalAttempts = 0;
        boolean degraded = false;

        for (String candidate : candidates) {
            Circuit circuit = circuits.computeIfAbsent(candidate, k -> new Circuit());
            if (isOpen(circuit, System.currentTimeMillis())) {
                // 熔断期内直接跳过该承运商，视为已降级。
                degraded = true;
                continue;
            }
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                totalAttempts++;
                try {
                    call.apply(candidate);
                    onSuccess(circuit);
                    return new CallOutcome<>(true, candidate, totalAttempts,
                            degraded || !candidate.equals(primaryCarrierId), null);
                } catch (RuntimeException e) {
                    lastError = e.getMessage();
                    onFailure(circuit, System.currentTimeMillis());
                    if (attempt < maxAttempts) {
                        sleepBackoff(attempt);
                    }
                }
            }
            degraded = true;
        }
        return new CallOutcome<>(false, null, totalAttempts, true, lastError);
    }

    private boolean isOpen(Circuit circuit, long nowMillis) {
        synchronized (circuit) {
            return circuit.openUntilMillis > nowMillis;
        }
    }

    private void onSuccess(Circuit circuit) {
        synchronized (circuit) {
            circuit.consecutiveFailures = 0;
            circuit.openUntilMillis = 0L;
        }
    }

    private void onFailure(Circuit circuit, long nowMillis) {
        synchronized (circuit) {
            circuit.consecutiveFailures++;
            if (circuit.consecutiveFailures >= failureThreshold) {
                circuit.openUntilMillis = nowMillis + cooldownMillis;
            }
        }
    }

    private void sleepBackoff(int attempt) {
        long delay = Math.min(maxBackoffMillis, (long) (baseBackoffMillis * Math.pow(2, attempt - 1)));
        if (delay <= 0) {
            return;
        }
        try {
            sleeper.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
