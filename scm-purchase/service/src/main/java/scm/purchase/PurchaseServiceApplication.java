package scm.purchase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Purchase Service Application
 *
 * Handles purchase requisitions, RFQ (Request for Quotation), contracts,
 * purchase orders and receipt management.
 *
 * @author SCM Platform Team
 */
@SpringBootApplication
@EnableDiscoveryClient
public class PurchaseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PurchaseServiceApplication.class, args);
    }
}