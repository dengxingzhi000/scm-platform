package com.scmcloud.file.service.upload;

import com.scmcloud.file.api.enums.UploadTaskStatus;
import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.entity.UploadTask;
import com.scmcloud.file.mapper.UploadTaskMapper;
import com.scmcloud.file.service.metadata.FileMetadataService;
import com.scmcloud.file.service.storage.StorageEngine;
import com.scmcloud.file.service.storage.StorageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {
    
    private final StorageFactory storageFactory;
    private final FileMetadataService metadataService;
    private final InstantUploadService instantUploadService;
    private final UploadTaskMapper uploadTaskMapper;
    
    @Transactional
    public FileMetadata upload(byte[] fileBytes, String fileName, String contentType,
                               String bizType, String bizId, Long tenantId) {
        String md5 = DigestUtils.md5Hex(fileBytes);
        
        return instantUploadService.checkExist(md5, tenantId)
                .orElseGet(() -> doUpload(fileBytes, fileName, contentType, md5, bizType, bizId, tenantId));
    }
    
    private FileMetadata doUpload(byte[] fileBytes, String fileName, String contentType,
                                   String md5, String bizType, String bizId, Long tenantId) {
        StorageEngine engine = storageFactory.getDefaultEngine();
        FileMetadata metadata = engine.upload(fileBytes, fileName, contentType, tenantId);
        metadata.setMd5(md5);
        metadata.setFileSize((long) fileBytes.length);
        metadata.setVersion(1);
        metadata.setBizType(bizType);
        metadata.setBizId(bizId);
        metadata.setCreateBy(tenantId);
        metadataService.saveMetadata(metadata);
        return metadata;
    }
    
    public String initMultipartUpload(String fileName, Long fileSize, Long tenantId) {
        UploadTask task = new UploadTask();
        task.setId(UUID.randomUUID().toString());
        task.setFileName(fileName);
        task.setFileSize(fileSize);
        task.setStatus(UploadTaskStatus.INIT);
        task.setTenantId(tenantId);
        uploadTaskMapper.insert(task);
        return task.getId();
    }
}
