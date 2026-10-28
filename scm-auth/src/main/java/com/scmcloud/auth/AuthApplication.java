package com.scmcloud.auth;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 认证服务
 *
 * @author Deng
 * createData 2025/10/11 10:16
 * @version 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@MapperScan({"com.scmcloud.common.log.mapper", "com.scmcloud.auth.mapper"})
@EnableTransactionManagement
@EnableCaching
@EnableScheduling
// Scanned "com.scmcloud" once, which pulled in scm-audit-service's commandService while its
// mapper package was not in @MapperScan, so the context failed with "No qualifying bean of type
// SysAuditLogMapper". Keep the scan to this service plus the shared library.
@ComponentScan(basePackages = {"com.scmcloud.auth", "com.scmcloud.common"})
@EnableDubbo(scanBasePackages = "com.scmcloud")
public class AuthApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
