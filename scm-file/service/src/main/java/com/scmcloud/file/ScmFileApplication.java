package com.scmcloud.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.scmcloud")
public class ScmFileApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmFileApplication.class, args);
    }
}
