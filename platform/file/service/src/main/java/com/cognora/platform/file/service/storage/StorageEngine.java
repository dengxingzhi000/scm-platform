package com.cognora.platform.file.service.storage;

import com.cognora.platform.file.api.enums.StorageType;
import com.cognora.platform.file.entity.FileMetadata;
import java.io.InputStream;
import java.time.Duration;

public interface StorageEngine {

    FileMetadata upload(InputStream content, long fileSize, String fileName, String contentType, String tenantId);

    InputStream download(String fileKey);

    String generatePresignedUrl(String fileKey, Duration expiry);

    void delete(String fileKey);

    boolean exists(String fileKey);

    StorageType support();
}
