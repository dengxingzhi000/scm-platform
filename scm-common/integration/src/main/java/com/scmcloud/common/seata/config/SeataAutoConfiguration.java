package com.scmcloud.common.seata.config;

import io.seata.spring.annotation.GlobalTransactionScanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seata 鑷姩閰嶇疆锟?
 *
 * <p>涓哄井鏈嶅姟鎻愪緵鍒嗗竷寮忎簨鍔¤兘鍔涖€傛敮锟紸T銆乀CC銆丼AGA銆乆A 妯″紡锟?
 *
 * <p>浣跨敤鏂瑰紡锟?
 * <pre>
 * &#64;GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
 * public Order createOrder(OrderDTO dto) {
 *     // 涓氬姟閫昏緫
 * }
 * </pre>
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Configuration
@ConditionalOnProperty(prefix = "seata", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SeataAutoConfiguration {

    @Value("${spring.application.name}")
    private String applicationId;

    @Value("${seata.tx-service-group:${spring.application.name}-tx-group}")
    private String txServiceGroup;

    /**
     * 鍏ㄥ眬浜嬪姟鎵弿锟?
     *
     * @return GlobalTransactionScanner
     */
    @Bean
    public GlobalTransactionScanner globalTransactionScanner() {
        return new GlobalTransactionScanner(applicationId, txServiceGroup);
    }
}