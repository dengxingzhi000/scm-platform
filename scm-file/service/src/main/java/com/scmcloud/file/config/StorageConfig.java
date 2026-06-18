package com.scmcloud.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "storage.minio")
public class StorageConfig {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private boolean createBucket = true;
}
