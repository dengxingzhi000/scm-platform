package com.cognora.platform.file.rpc;

import com.cognora.platform.file.api.FileManageApi;
import com.cognora.platform.file.api.dto.FileMetadataDTO;
import com.cognora.platform.file.convert.FileMetadataConvert;
import com.cognora.platform.file.entity.FileMetadata;
import com.cognora.platform.file.service.management.FileManagementService;
import com.cognora.platform.file.service.metadata.FileMetadataService;
import com.cognora.platform.file.service.upload.UploadService;
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
    private final FileManagementService fileManagementService;

    @Override
    public void delete(String id, String tenantId) {
        Optional<FileMetadata> meta = metadataService.findById(id, tenantId);
        meta.ifPresent(m -> fileManagementService.delete(m.getId(), tenantId));
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
