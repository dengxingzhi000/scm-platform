package com.cognora.platform.notify;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Notification Service Application
 *
 * Handles multi-channel notifications including email, SMS and in-app messages.
 * Supports notification templates, user preferences and delivery tracking.
 *
 * @author SCM Platform Team
 */
@SpringBootApplication(scanBasePackages = {"com.cognora.platform.notify", "com.cognora.platform.common"})
@EnableDiscoveryClient
@MapperScan("com.cognora.platform.notify.mapper")
public class NotifyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotifyServiceApplication.class, args);
    }
}