package com.scmcloud.finance;

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
@SpringBootApplication(scanBasePackages = {"com.scmcloud.finance", "com.scmcloud.common"})
@EnableDiscoveryClient
@MapperScan("com.scmcloud.finance.mapper")
public class FinanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceServiceApplication.class, args);
    }
}