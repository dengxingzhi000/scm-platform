package com.cognora.platform.file.service.upload;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.file.entity.FileMetadata;
import com.cognora.platform.file.service.metadata.FileMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstantUploadService {
    private final FileMetadataService metadataService;
    
    /**
     * Web 入口：校验当前租户下是否已存在相同 MD5 的文件。
     */
    public Optional<FileMetadata> checkExist(String md5) {
        return checkExist(md5, TenantContextHolder.getRequiredTenantId().toString());
    }

    public Optional<FileMetadata> checkExist(String md5, String tenantId) {
        return metadataService.findByMd5(md5, tenantId);
    }
}
