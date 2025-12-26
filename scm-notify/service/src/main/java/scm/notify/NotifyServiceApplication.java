package scm.notify;

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
@SpringBootApplication
@EnableDiscoveryClient
public class NotifyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotifyServiceApplication.class, args);
    }
}