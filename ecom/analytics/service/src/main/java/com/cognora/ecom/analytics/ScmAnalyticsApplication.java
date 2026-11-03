package com.cognora.ecom.analytics;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {
        "com.cognora.ecom.analytics",
        "com.cognora.platform.common"
})
@EnableDiscoveryClient
@EnableDubbo
@EnableScheduling
@EnableTransactionManagement
public class ScmAnalyticsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScmAnalyticsApplication.class, args);
    }
}
