package com.scmcloud.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.scmcloud.analytics",
        "com.scmcloud.common.integration",
        "com.scmcloud.common.cache",
        "com.scmcloud.common.tenant"
})
@EnableScheduling
public class ScmAnalyticsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScmAnalyticsApplication.class, args);
    }
}
