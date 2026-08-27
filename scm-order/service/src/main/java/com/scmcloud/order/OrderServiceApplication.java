package com.scmcloud.order;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import java.util.UUID;

@SpringBootApplication(scanBasePackages = {"com.scmcloud.order", "com.scmcloud.common"})
@EnableDiscoveryClient
@EnableDubbo
@EnableTransactionManagement
@MapperScan("com.scmcloud.order.mapper")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
