package com.scmcloud.file.service.download;

import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.service.FileNotFoundException;
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

    /**
     * Web 入口：按当前租户解析文件元数据。
     */
    public FileMetadata resolve(String fileId) {
        return resolve(fileId, currentTenantId());
    }

    public FileMetadata resolve(String fileId, String tenantId) {
        return metadataService.findById(fileId, tenantId)
                .orElseThrow(() -> new FileNotFoundException(fileId));
    }

    /**
     * Web 入口：为当前租户的文件生成预签名访问地址。
     */
    public String generatePresignedUrl(String fileId) {
        return generatePresignedUrl(fileId, currentTenantId());
    }

    public String generatePresignedUrl(String fileId, String tenantId) {
        FileMetadata metadata = resolve(fileId, tenantId);
        StorageEngine engine = storageFactory.getDefaultEngine();
        Duration expiry = storageConfig.getPresignedUrlExpiry();
        return engine.generatePresignedUrl(metadata.getStorageKey(), expiry);
    }

    /**
     * Web 入口：打开当前租户的文件内容流。
     */
    public InputStream openContent(String fileId) {
        return openContent(fileId, currentTenantId());
    }

    public InputStream openContent(String fileId, String tenantId) {
        FileMetadata metadata = resolve(fileId, tenantId);
        StorageEngine engine = storageFactory.getDefaultEngine();
        return engine.download(metadata.getStorageKey());
    }

    private String currentTenantId() {
        return TenantContextHolder.getRequiredTenantId().toString();
    }
}
