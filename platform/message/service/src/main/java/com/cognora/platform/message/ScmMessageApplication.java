package com.cognora.platform.message;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.cognora.platform.message", "com.cognora.platform.common"})
@EnableScheduling
@MapperScan("com.cognora.platform.message.mapper")
public class ScmMessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmMessageApplication.class, args);
    }
}
