package com.scmcloud.file.service.storage;

import com.scmcloud.file.api.enums.StorageType;
import com.scmcloud.file.config.StorageConfig;
import com.scmcloud.file.entity.FileMetadata;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioStorageEngine implements StorageEngine {
    private final MinioClient minioClient;
    private final StorageConfig config;

    @Override
    public FileMetadata upload(InputStream content, long fileSize, String fileName, String contentType, String tenantId) {
        try {
            ensureBucket();
            String storageKey = generateStorageKey(tenantId, fileName);

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(config.getMinio().getBucketName())
                    .object(storageKey)
                    .stream(content, fileSize, -1)
                    .contentType(contentType)
                    .build());

            FileMetadata metadata = new FileMetadata();
            metadata.setOriginalName(fileName);
            metadata.setStorageKey(storageKey);
            metadata.setContentType(contentType);
            metadata.setFileSize(fileSize);
            metadata.setStorageEngine("MINIO");
            metadata.setTenantId(tenantId);

            return metadata;
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO, fileName={}, tenantId={}", fileName, tenantId, e);
            throw new com.scmcloud.file.service.storage.StorageException("File upload failed", e);
        }
    }

    @Override
    public InputStream download(String fileKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(config.getMinio().getBucketName())
                    .object(fileKey)
                    .build());
        } catch (Exception e) {
            log.error("Failed to download file from MinIO, fileKey={}", fileKey, e);
            throw new StorageException("File download failed", e);
        }
    }

    @Override
    public String generatePresignedUrl(String fileKey, Duration expiry) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(config.getMinio().getBucketName())
                    .object(fileKey)
                    .expiry((int) expiry.toSeconds())
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL, fileKey={}", fileKey, e);
            throw new StorageException("URL generation failed", e);
        }
    }

    @Override
    public void delete(String fileKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(config.getMinio().getBucketName())
                    .object(fileKey)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO, fileKey={}", fileKey, e);
            throw new StorageException("File deletion failed", e);
        }
    }

    @Override
    public boolean exists(String fileKey) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(config.getMinio().getBucketName())
                    .object(fileKey)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public StorageType support() {
        return StorageType.MINIO;
    }

    private void ensureBucket() {
        if (!config.getMinio().isCreateBucket()) {
            return;
        }
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(config.getMinio().getBucketName()).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(config.getMinio().getBucketName()).build());
            }
        } catch (Exception e) {
            log.warn("Unable to ensure bucket {} exists, will retry on put", config.getMinio().getBucketName(), e);
        }
    }

    private String generateStorageKey(String tenantId, String fileName) {
        String extension = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = fileName.substring(lastDot);
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return tenantId + "/" + datePath + "/" + UUID.randomUUID() + extension;
    }
}
