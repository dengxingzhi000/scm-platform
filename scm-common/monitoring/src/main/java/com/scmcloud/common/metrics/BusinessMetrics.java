package com.scmcloud.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 鑷畾涔夋寚锟?
 *
 * @author Deng
 * createData 2025/10/22 14:01
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class BusinessMetrics {
    private final MeterRegistry registry;

    // 缂撳瓨鎸囨爣瀵硅薄
    private final Map<String, Counter> counterCache = new ConcurrentHashMap<>();
    private final Map<String, Timer> timerCache = new ConcurrentHashMap<>();
    private final Map<String, DistributionSummary> summaryCache = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> gaugeCache = new ConcurrentHashMap<>();

    /**
     * 璁板綍涓氬姟鎸囨爣 - 璁℃暟锟?
     * 绀轰緥锛氱櫥褰曟鏁般€佽鍗曟暟閲忋€佹敮浠樻锟?
     */
    public void recordCount(String metricName, String... tags) {
        String key = metricName + String.join(",", tags);
        counterCache.computeIfAbsent(key, k ->
                Counter.builder(metricName)
                        .tags(tags)
                        .description("Business count metric: " + metricName)
                        .register(registry)
        ).increment();
    }

    /**
     * 璁板綍涓氬姟鎸囨爣 - 璁℃椂锟?
     * 绀轰緥锛氭帴鍙ｈ€楁椂銆佷笟鍔″鐞嗘椂锟?
     */
    public void recordTime(String metricName, long timeMs, String... tags) {
        String key = metricName + String.join(",", tags);
        timerCache.computeIfAbsent(key, k ->
                Timer.builder(metricName)
                        .tags(tags)
                        .description("Business timing metric: " + metricName)
                        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                        .publishPercentileHistogram()
                        .register(registry)
        ).record(timeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 璁板綍涓氬姟鎸囨爣 - 鍒嗗竷鎽樿
     * 绀轰緥锛氳鍗曢噾棰濆垎甯冦€佽姹備綋澶у皬
     */
    public void recordDistribution(String metricName, double value, String... tags) {
        String key = metricName + String.join(",", tags);
        summaryCache.computeIfAbsent(key, k ->
                DistributionSummary.builder(metricName)
                        .tags(tags)
                        .description("Business distribution metric: " + metricName)
                        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                        .register(registry)
        ).record(value);
    }

    /**
     * 璁板綍涓氬姟鎸囨爣 - 浠〃锟?
     * 绀轰緥锛氬湪绾跨敤鎴锋暟銆侀槦鍒楅暱搴︺€佺紦瀛樺懡涓巼
     */
    public void recordGauge(String metricName, long value, String... tags) {
        String key = metricName + String.join(",", tags);
        AtomicLong atomicValue = gaugeCache.computeIfAbsent(key, k -> {
            AtomicLong atomic = new AtomicLong(value);
            Gauge.builder(metricName, atomic, AtomicLong::get)
                    .tags(tags)
                    .description("Business status metric: " + metricName)
                    .register(registry);
            return atomic;
        });
        atomicValue.set(value);
    }

    /**
     * 蹇嵎鏂规硶 - 璁板綍鐧诲綍
     */
    public void recordLogin(boolean success, String source) {
        recordCount("business.login.total", "success", String.valueOf(success), "source", source);
    }

    /**
     * 蹇嵎鏂规硶 - 璁板綍API璋冪敤
     */
    public void recordApi(String api, long timeMs, boolean success) {
        recordCount("business.api.calls", "api", api, "success", String.valueOf(success));
        recordTime("business.api.duration", timeMs, "api", api);
    }

    /**
     * 蹇嵎鏂规硶 - 璁板綍缂撳瓨鍛戒腑
     */
    public void recordCache(String cacheName, boolean hit) {
        recordCount("business.cache.access", "cache", cacheName, "hit", String.valueOf(hit));
    }

    /**
     * 鐧诲綍鎴愬姛锟?
     */
    public void recordLoginAttempt(boolean success, String reason) {
        Counter.builder("business.login.attempts")
                .tag("success", String.valueOf(success))
                .tag("reason", reason)
                .register(registry)
                .increment();
    }

    /**
     * 鏉冮檺鎺堜簣瀹¤
     */
    public void recordPermissionGrant(String type, int count) {
        Counter.builder("business.permission.grants")
                .tag("type", type)
                .register(registry)
                .increment(count);
    }

    /**
     * 涓存椂鏉冮檺杩囨湡棰勮
     */
    public void recordExpiringPermissions(int count) {
        Gauge.builder("business.permissions.expiring", () -> count)
                .description("Number of expiring temporary permissions")
                .register(registry);
    }
}
