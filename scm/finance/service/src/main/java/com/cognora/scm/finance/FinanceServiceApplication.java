package com.cognora.scm.finance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Finance Service Application

 * Handles freight management, settlement reconciliation, invoice management
 * and payment processing.
 *
 * @author SCM Platform Team
 */
@SpringBootApplication(scanBasePackages = {"com.cognora.scm.finance", "com.cognora.platform.common"})
@EnableDiscoveryClient
@MapperScan("com.cognora.scm.finance.mapper")
public class FinanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceServiceApplication.class, args);
    }
}