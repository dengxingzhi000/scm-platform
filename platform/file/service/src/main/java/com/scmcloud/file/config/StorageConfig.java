package com.scmcloud.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageConfig {
    private Minio minio = new Minio();
    private long maxFileSize = 100 * 1024 * 1024;
    private List<String> allowedExtensions = List.of();
    private List<String> allowedContentTypes = List.of();
    private Duration presignedUrlExpiry = Duration.ofMinutes(30);
    private VirusScan virusScan = new VirusScan();

    @Data
    public static class Minio {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketName;
        private boolean createBucket = true;
    }

    @Data
    public static class VirusScan {
        private boolean enabled = false;
        private String clamdHost = "localhost";
        private int clamdPort = 3310;
    }
}
