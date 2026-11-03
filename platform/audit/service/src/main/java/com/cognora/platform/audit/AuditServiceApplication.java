package com.cognora.platform.audit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Audit Service Application
 * Handles operation audit logs, sensitive operation tracking and security monitoring.
 * Uses partitioned tables for efficient historical data management.
 *
 * @author SCM Platform Team
 */
@SpringBootApplication(scanBasePackages = {"com.cognora.platform.audit", "com.cognora.platform.common"})
@EnableDiscoveryClient
@MapperScan("com.cognora.platform.audit.mapper")
public class AuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}