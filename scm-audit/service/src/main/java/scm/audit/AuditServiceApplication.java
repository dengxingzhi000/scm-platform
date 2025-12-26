package scm.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Audit Service Application
 * Handles operation audit logs, sensitive operation tracking and security monitoring.
 * Uses partitioned tables for efficient historical data management.
 *
 * @author SCM Platform Team
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}