package com.scmcloud.file.rpc;

import com.scmcloud.file.api.FileQueryApi;
import com.scmcloud.file.api.dto.FileMetadataDTO;
import com.scmcloud.file.convert.FileMetadataConvert;
import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.service.metadata.FileMetadataService;
import com.scmcloud.file.service.storage.StorageEngine;
import com.scmcloud.file.service.storage.StorageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@DubboService
public class FileQueryApiImpl implements FileQueryApi {

    private final FileMetadataService metadataService;
    private final FileMetadataConvert metadataConvert;
    private final StorageFactory storageFactory;

    @Override
    public FileMetadataDTO getById(String id, String tenantId) {
        return metadataService.findById(id, tenantId)
                .map(metadataConvert::toDTO)
                .orElse(null);
    }

    @Override
    public FileMetadataDTO getByMd5(String md5, String tenantId) {
        return metadataService.findByMd5(md5, tenantId)
                .map(metadataConvert::toDTO)
                .orElse(null);
    }

    @Override
    public List<FileMetadataDTO> getByBizId(String bizType, String bizId, String tenantId) {
        return metadataService.findByBizId(bizType, bizId, tenantId).stream()
                .map(metadataConvert::toDTO)
                .toList();
    }

    @Override
    public String generatePresignedUrl(String fileKey, String tenantId) {
        StorageEngine engine = storageFactory.getDefaultEngine();
        return engine.generatePresignedUrl(fileKey, java.time.Duration.ofMinutes(30));
    }

    @Override
    public byte[] download(String fileKey, String tenantId) {
        StorageEngine engine = storageFactory.getDefaultEngine();
        try (InputStream in = engine.download(fileKey)) {
            return in.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to download file, fileKey={}, tenantId={}", fileKey, tenantId, e);
            throw new RuntimeException("Failed to download file: " + fileKey, e);
        }
    }
}
