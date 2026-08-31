package com.scmcloud.file.service.download;

import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.service.metadata.FileMetadataService;
import com.scmcloud.file.service.storage.StorageEngine;
import com.scmcloud.file.service.storage.StorageFactory;
import com.scmcloud.file.config.StorageConfig;
import com.scmcloud.file.service.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadService {

    private final FileMetadataService metadataService;
    private final StorageFactory storageFactory;
    private final StorageConfig storageConfig;

    public FileMetadata resolve(String fileId, String tenantId) {
        return metadataService.findById(fileId, tenantId)
                .orElseThrow(() -> new FileNotFoundException("File not found: " + fileId));
    }

    public String generatePresignedUrl(String fileId, String tenantId) {
        FileMetadata metadata = resolve(fileId, tenantId);
        StorageEngine engine = storageFactory.getDefaultEngine();
        Duration expiry = storageConfig.getPresignedUrlExpiry();
        return engine.generatePresignedUrl(metadata.getStorageKey(), expiry);
    }

    public InputStream openContent(String fileId, String tenantId) {
        FileMetadata metadata = resolve(fileId, tenantId);
        StorageEngine engine = storageFactory.getDefaultEngine();
        return engine.download(metadata.getStorageKey());
    }

    public static class FileNotFoundException extends RuntimeException {
        public FileNotFoundException(String message) {
            super(message);
        }
    }
}
