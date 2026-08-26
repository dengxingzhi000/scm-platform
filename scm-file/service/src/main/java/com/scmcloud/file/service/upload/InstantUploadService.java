package com.scmcloud.file.service.upload;

import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.service.metadata.FileMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstantUploadService {
    private final FileMetadataService metadataService;
    
    public Optional<FileMetadata> checkExist(String md5, Long tenantId) {
        return metadataService.findByMd5(md5, tenantId);
    }
}
