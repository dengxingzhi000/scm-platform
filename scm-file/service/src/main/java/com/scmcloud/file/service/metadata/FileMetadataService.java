package com.scmcloud.file.service.metadata;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.file.api.dto.FileStatsDTO;
import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.mapper.FileMetadataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class FileMetadataService extends ServiceImpl<FileMetadataMapper, FileMetadata> {
    
    public FileMetadata saveMetadata(FileMetadata metadata) {
        save(metadata);
        return metadata;
    }
    
    public Optional<FileMetadata> findByMd5(String md5, String tenantId) {
        LambdaQueryWrapper<FileMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileMetadata::getMd5, md5)
               .eq(FileMetadata::getTenantId, tenantId);
        return Optional.ofNullable(getOne(wrapper));
    }
    
    public Optional<FileMetadata> findById(String id, String tenantId) {
        LambdaQueryWrapper<FileMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileMetadata::getId, id)
               .eq(FileMetadata::getTenantId, tenantId);
        return Optional.ofNullable(getOne(wrapper));
    }

    public List<FileMetadata> findByBizId(String bizType, String bizId, String tenantId) {
        LambdaQueryWrapper<FileMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileMetadata::getBizType, bizType)
               .eq(FileMetadata::getBizId, bizId)
               .eq(FileMetadata::getTenantId, tenantId);
        return list(wrapper);
    }

    /**
     * 统计当前租户的文件总数、占用空间与类型分布。
     */
    public FileStatsDTO getStats(String tenantId) {
        FileStatsDTO stats = baseMapper.selectStats(tenantId);
        if (stats == null) {
            stats = new FileStatsDTO();
            stats.setTotalFiles(0L);
            stats.setTotalSize(0L);
            stats.setImageFiles(0L);
            stats.setDocumentFiles(0L);
            stats.setOtherFiles(0L);
            return stats;
        }
        long total = stats.getTotalFiles() == null ? 0L : stats.getTotalFiles();
        long image = stats.getImageFiles() == null ? 0L : stats.getImageFiles();
        long document = stats.getDocumentFiles() == null ? 0L : stats.getDocumentFiles();
        stats.setTotalFiles(total);
        stats.setTotalSize(stats.getTotalSize() == null ? 0L : stats.getTotalSize());
        stats.setImageFiles(image);
        stats.setDocumentFiles(document);
        stats.setOtherFiles(Math.max(0L, total - image - document));
        return stats;
    }
}
