package com.scmcloud.system;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.scmcloud.system", "com.scmcloud.common"})
@EnableDiscoveryClient
@EnableDubbo
@EnableCaching
@EnableScheduling
@MapperScan({"com.scmcloud.system.mapper", "com.scmcloud.common.log.mapper"})
public class SysServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SysServiceApplication.class, args);
    }
}
