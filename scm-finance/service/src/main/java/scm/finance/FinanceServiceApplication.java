package scm.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Finance Service Application

 * Handles freight management, settlement reconciliation, invoice management
 * and payment processing.
 *
 * @author SCM Platform Team
 */
@SpringBootApplication
@EnableDiscoveryClient
public class FinanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceServiceApplication.class, args);
    }
}