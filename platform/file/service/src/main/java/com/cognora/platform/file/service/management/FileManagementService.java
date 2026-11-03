package com.cognora.platform.file.service.management;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.file.entity.FileMetadata;
import com.cognora.platform.file.service.FileNotFoundException;
import com.cognora.platform.file.service.metadata.FileMetadataService;
import com.cognora.platform.file.service.storage.StorageFactory;
import com.cognora.platform.file.service.upload.FileValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 文件生命周期管理：重命名、调整业务关联与删除。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileManagementService {

    private final FileMetadataService metadataService;
    private final StorageFactory storageFactory;

    /**
     * Web 入口：更新当前租户的文件元数据。
     */
    public FileMetadata update(String id, String originalName, String bizType, String bizId) {
        return update(id, currentTenantId(), originalName, bizType, bizId);
    }

    /**
     * 更新文件元数据。bizType / bizId 传空表示解除业务关联。
     */
    public FileMetadata update(String id, String tenantId, String originalName,
                               String bizType, String bizId) {
        FileMetadata metadata = metadataService.findById(id, tenantId)
                .orElseThrow(() -> new FileNotFoundException(id));

        if (originalName != null) {
            String name = originalName.trim();
            if (name.isEmpty()) {
                throw new FileValidationException("File name cannot be empty");
            }
            metadata.setOriginalName(name);
        }
        metadata.setBizType(trimToNull(bizType));
        metadata.setBizId(trimToNull(bizId));
        metadataService.updateById(metadata);
        return metadata;
    }

    /**
     * Web 入口：删除当前租户的单个文件。
     */
    public void delete(String id) {
        delete(id, currentTenantId());
    }

    /**
     * 删除单个文件。存储对象删除失败只记录日志，元数据仍会移除，避免用户无法清理记录。
     */
    public void delete(String id, String tenantId) {
        FileMetadata metadata = metadataService.findById(id, tenantId)
                .orElseThrow(() -> new FileNotFoundException(id));
        deleteStorageObject(metadata);
        metadataService.removeById(metadata.getId());
    }

    /**
     * Web 入口：批量删除当前租户下的文件。
     */
    public int deleteBatch(List<String> ids) {
        return deleteBatch(ids, currentTenantId());
    }

    /**
     * 批量删除当前租户下的文件，返回实际删除的数量。
     */
    public int deleteBatch(List<String> ids, String tenantId) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<String> targets = ids.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (targets.isEmpty()) {
            return 0;
        }
        List<FileMetadata> files = metadataService.list(new QueryWrapper<FileMetadata>()
                .in("id", targets)
                .eq("tenant_id", tenantId));
        files.forEach(this::deleteStorageObject);
        if (!files.isEmpty()) {
            metadataService.removeByIds(files.stream().map(FileMetadata::getId).toList());
        }
        return files.size();
    }

    private void deleteStorageObject(FileMetadata metadata) {
        String key = metadata.getStorageKey();
        if (!StringUtils.hasText(key)) {
            return;
        }
        try {
            storageFactory.getDefaultEngine().delete(key);
        } catch (Exception e) {
            log.warn("Failed to remove storage object for file {}, key={}", metadata.getId(), key, e);
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String currentTenantId() {
        return TenantContextHolder.getRequiredTenantId().toString();
    }
}