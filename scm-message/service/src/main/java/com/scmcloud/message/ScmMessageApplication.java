package com.scmcloud.message;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.scmcloud.message", "com.scmcloud.common"})
@EnableScheduling
@MapperScan("com.scmcloud.message.mapper")
public class ScmMessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmMessageApplication.class, args);
    }
}
