package com.scmcloud.file.service.storage;

import com.scmcloud.file.api.enums.StorageType;
import com.scmcloud.file.entity.FileMetadata;
import java.io.InputStream;
import java.time.Duration;

public interface StorageEngine {
    
    FileMetadata upload(byte[] fileBytes, String fileName, String contentType, Long tenantId);
    
    InputStream download(String fileKey);
    
    String generatePresignedUrl(String fileKey, Duration expiry);
    
    void delete(String fileKey);
    
    boolean exists(String fileKey);
    
    StorageType support();
}