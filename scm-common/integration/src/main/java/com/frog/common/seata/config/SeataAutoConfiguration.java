package com.frog.common.seata.config;

import io.seata.spring.annotation.GlobalTransactionScanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seata 自动配置类
 *
 * <p>为微服务提供分布式事务能力。支持 AT、TCC、SAGA、XA 模式。
 *
 * <p>使用方式：
 * <pre>
 * &#64;GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
 * public Order createOrder(OrderDTO dto) {
 *     // 业务逻辑
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
     * 全局事务扫描器
     *
     * @return GlobalTransactionScanner
     */
    @Bean
    public GlobalTransactionScanner globalTransactionScanner() {
        return new GlobalTransactionScanner(applicationId, txServiceGroup);
    }
}