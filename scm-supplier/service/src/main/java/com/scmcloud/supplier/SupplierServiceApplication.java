package com.scmcloud.supplier;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.scmcloud.supplier", "com.scmcloud.common"})
@EnableDiscoveryClient
@EnableScheduling
@EnableTransactionManagement
@MapperScan("com.scmcloud.supplier.mapper")
public class SupplierServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupplierServiceApplication.class, args);
    }
}