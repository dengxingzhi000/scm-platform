package com.scmcloud.purchase;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Purchase Service Application
 *
 * Handles purchase requisitions, RFQ (Request for Quotation), contracts,
 * purchase orders and receipt management.
 *
 * @author SCM Platform Team
 */
@SpringBootApplication(scanBasePackages = {"com.scmcloud.purchase", "com.scmcloud.common"})
@EnableDiscoveryClient
@MapperScan("com.scmcloud.purchase.mapper")
public class PurchaseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PurchaseServiceApplication.class, args);
    }
}