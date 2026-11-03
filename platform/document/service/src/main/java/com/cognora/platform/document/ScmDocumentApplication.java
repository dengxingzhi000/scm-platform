package com.cognora.platform.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.cognora.platform.document", "com.cognora.platform.common"})
public class ScmDocumentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmDocumentApplication.class, args);
    }
}
