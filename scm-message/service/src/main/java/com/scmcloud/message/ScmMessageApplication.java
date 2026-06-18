package com.scmcloud.message;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScmMessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmMessageApplication.class, args);
    }
}
