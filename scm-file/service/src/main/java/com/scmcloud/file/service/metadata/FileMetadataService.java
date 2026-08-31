package com.scmcloud.file.service.metadata;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
}
