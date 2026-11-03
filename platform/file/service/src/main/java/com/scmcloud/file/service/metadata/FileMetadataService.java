package com.scmcloud.file.service.metadata;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.file.api.dto.FileMetadataDTO;
import com.scmcloud.file.api.dto.FileStatsDTO;
import com.scmcloud.file.convert.FileMetadataConvert;
import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.mapper.FileMetadataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class FileMetadataService extends ServiceImpl<FileMetadataMapper, FileMetadata> {

    private final FileMetadataConvert fileMetadataConvert;

    public FileMetadataService(FileMetadataConvert fileMetadataConvert) {
        this.fileMetadataConvert = fileMetadataConvert;
    }

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
     * Web 入口：分页查询当前租户的文件元数据，按名称关键字、业务类型和业务 ID 过滤。
     */
    public Page<FileMetadataDTO> pageFiles(int page, int size, String keyword, String bizType, String bizId) {
        String tenantId = currentTenantId();
        LambdaQueryWrapper<FileMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileMetadata::getTenantId, tenantId)
                .like(StringUtils.hasText(keyword), FileMetadata::getOriginalName, keyword)
                .eq(StringUtils.hasText(bizType), FileMetadata::getBizType, bizType)
                .eq(StringUtils.hasText(bizId), FileMetadata::getBizId, bizId)
                .orderByDesc(FileMetadata::getCreateTime);
        Page<FileMetadata> metaPage = page(new Page<>(page, size), wrapper);
        Page<FileMetadataDTO> dtoPage = new Page<>(metaPage.getCurrent(), metaPage.getSize(), metaPage.getTotal());
        dtoPage.setRecords(metaPage.getRecords().stream().map(fileMetadataConvert::toDTO).toList());
        return dtoPage;
    }

    /**
     * Web 入口：统计当前租户的文件总数、占用空间与类型分布。
     */
    public FileStatsDTO getStats() {
        return getStats(currentTenantId());
    }

    /**
     * 统计指定租户的文件总数、占用空间与类型分布。
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

    private String currentTenantId() {
        return TenantContextHolder.getRequiredTenantId().toString();
    }
}
