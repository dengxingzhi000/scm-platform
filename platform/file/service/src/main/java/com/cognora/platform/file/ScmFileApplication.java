package com.cognora.platform.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.cognora.platform.file", "com.cognora.platform.common"})
@MapperScan("com.cognora.platform.file.mapper")
public class ScmFileApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmFileApplication.class, args);
    }
}
