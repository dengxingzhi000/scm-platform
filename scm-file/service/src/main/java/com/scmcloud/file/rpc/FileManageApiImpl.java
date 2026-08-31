package com.scmcloud.file.rpc;

import com.scmcloud.file.api.FileManageApi;
import com.scmcloud.file.api.dto.FileMetadataDTO;
import com.scmcloud.file.convert.FileMetadataConvert;
import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.service.metadata.FileMetadataService;
import com.scmcloud.file.service.storage.StorageEngine;
import com.scmcloud.file.service.storage.StorageFactory;
import com.scmcloud.file.service.upload.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@DubboService
public class FileManageApiImpl implements FileManageApi {

    private final UploadService uploadService;
    private final FileMetadataService metadataService;
    private final FileMetadataConvert metadataConvert;
    private final StorageFactory storageFactory;

    @Override
    public void delete(String id, String tenantId) {
        Optional<FileMetadata> meta = metadataService.findById(id, tenantId);
        meta.ifPresent(m -> {
            storageFactory.getDefaultEngine().delete(m.getStorageKey());
            metadataService.removeById(m.getId());
        });
    }

    @Override
    public void updateBizAssociation(String id, String bizType, String bizId, String tenantId) {
        metadataService.findById(id, tenantId).ifPresent(m -> {
            m.setBizType(bizType);
            m.setBizId(bizId);
            metadataService.updateById(m);
        });
    }

    @Override
    public FileMetadataDTO upload(byte[] content, String originalName, String contentType,
                                 String tenantId, String bizType, String bizId) {
        FileMetadata saved = uploadService.uploadBytes(content, originalName, contentType, bizType, bizId, tenantId);
        return metadataConvert.toDTO(saved);
    }
}
