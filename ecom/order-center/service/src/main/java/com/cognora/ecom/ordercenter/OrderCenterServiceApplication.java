package com.cognora.ecom.ordercenter;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.cognora.ecom.ordercenter", "com.cognora.platform.common"})
@EnableDiscoveryClient
@EnableDubbo
@EnableTransactionManagement
@MapperScan("com.cognora.ecom.ordercenter.mapper")
public class OrderCenterServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderCenterServiceApplication.class, args);
    }
}
