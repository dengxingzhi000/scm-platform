package com.scmcloud.tenant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Tenant Service Application
 *
 * Handles multi-tenant SaaS management including tenant configuration,
 * resource quotas, billing, and feature toggles.
 *
 * @author SCM Platform Team
 */
@SpringBootApplication(scanBasePackages = {"com.scmcloud.tenant", "com.scmcloud.common"})
@EnableDiscoveryClient
@MapperScan("com.scmcloud.tenant.mapper")
public class TenantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantServiceApplication.class, args);
    }
}