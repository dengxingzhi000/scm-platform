package com.cognora.scm.order;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import java.util.UUID;

@SpringBootApplication(scanBasePackages = {"com.cognora.scm.order", "com.cognora.platform.common"})
@EnableDiscoveryClient
@EnableDubbo
@EnableScheduling
@EnableTransactionManagement
@MapperScan("com.cognora.scm.order.mapper")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
