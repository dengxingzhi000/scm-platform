package com.scmcloud.system;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Service 妯″潡鍚姩锟?
 *
 * @author Deng
 * createData 2025/11/13 13:56
 * @version 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableDubbo
public class SysServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SysServiceApplication.class, args);
    }
}
