package com.scmcloud.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.scmcloud.document", "com.scmcloud.common"})
public class ScmDocumentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmDocumentApplication.class, args);
    }
}
