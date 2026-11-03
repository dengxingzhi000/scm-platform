package com.scmcloud.system.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 状态字典启动预热器。
 * 只加载 global (tenant_id=NULL) 数据，不做全租户预热。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatusDictCacheWarmer {

    private final StatusDictCacheManager cacheManager;

    @EventListener(ApplicationReadyEvent.class)
    @Order(10)
    public void onStartup() {
        cacheManager.prewarmGlobal();
    }
}
