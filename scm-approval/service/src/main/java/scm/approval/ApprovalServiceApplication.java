package scm.approval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Approval Service Application
 *
 * Handles permission approval workflows, approval process management
 * and custom workflow engine.
 *
 * @author SCM Platform Team
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApprovalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApprovalServiceApplication.class, args);
    }
}