package com.scmcloud.common.cache.warming;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@Order(1)
public class CacheWarmingConfig implements ApplicationRunner {

    private final List<CacheWarmer> warmers;

    public CacheWarmingConfig(List<CacheWarmer> warmers) {
        this.warmers = warmers;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        log.info("Starting cache warming with {} warmers", warmers.size());
        warmers.stream()
                .sorted(Comparator.comparingInt(CacheWarmer::getOrder))
                .forEach(warmer -> {
                    try {
                        log.info("Running cache warmer: {}", warmer.getWarmerName());
                        warmer.warmCache();
                        log.info("Cache warmer {} completed successfully", warmer.getWarmerName());
                    } catch (Exception e) {
                        log.error("Cache warmer {} failed: {}", warmer.getWarmerName(), e.getMessage(), e);
                    }
                });
        log.info("Cache warming completed");
    }
}