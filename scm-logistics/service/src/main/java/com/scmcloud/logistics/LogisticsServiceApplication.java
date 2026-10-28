package com.scmcloud.logistics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.scmcloud.logistics", "com.scmcloud.common"})
@EnableDiscoveryClient
@EnableTransactionManagement
@MapperScan("com.scmcloud.logistics.mapper")
public class LogisticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsServiceApplication.class, args);
    }
}