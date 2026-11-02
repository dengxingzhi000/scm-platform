package com.scmcloud.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.scmcloud.file", "com.scmcloud.common"})
@MapperScan("com.scmcloud.file.mapper")
public class ScmFileApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmFileApplication.class, args);
    }
}
