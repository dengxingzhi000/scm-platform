package com.scmcloud.document.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.document.file.domain.entity.DocumentFile;
import com.scmcloud.document.file.mapper.DocumentFileMapper;
import com.scmcloud.file.api.FileManageApi;
import com.scmcloud.file.api.FileQueryApi;
import com.scmcloud.file.api.dto.FileMetadataDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Service
public class DocumentFileService extends ServiceImpl<DocumentFileMapper, DocumentFile> {

    @DubboReference
    private FileQueryApi fileQueryApi;

    @DubboReference
    private FileManageApi fileManageApi;

    public Optional<DocumentFile> findById(String id, String tenantId) {
        return lambdaQuery()
                .eq(DocumentFile::getId, id)
                .eq(DocumentFile::getTenantId, tenantId)
                .oneOpt();
    }

    /**
     * 分页查询文档文件，支持按名称关键字、来源类型和关联业务 ID 过滤。
     */
    public Page<DocumentFile> pageQuery(int page, int size, String tenantId, String keyword,
                                        String sourceType, String refId) {
        LambdaQueryWrapper<DocumentFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentFile::getTenantId, tenantId)
                .like(StringUtils.hasText(keyword), DocumentFile::getOriginalName, keyword)
                .eq(StringUtils.hasText(sourceType), DocumentFile::getSourceType, sourceType)
                .eq(StringUtils.hasText(refId), DocumentFile::getRefId, refId)
                .orderByDesc(DocumentFile::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    /** 通过 scm-file 解析真实文件元数据 */
    public Optional<FileMetadataDTO> resolveStorage(String fileRef, String tenantId) {
        return Optional.ofNullable(fileQueryApi.getById(fileRef, tenantId));
    }

    /** 通过 scm-file 下载字节(渲染输入) */
    public byte[] downloadStorage(String fileRef, String tenantId) {
        FileMetadataDTO meta = fileQueryApi.getById(fileRef, tenantId);
        if (meta == null) {
            return null;
        }
        return fileQueryApi.download(meta.getStorageKey(), tenantId);
    }

    /** 通过 scm-file 上传渲染产物, 返回 scm-file 文件 id */
    public String storeOutput(byte[] content, String originalName, String contentType,
                              String tenantId, String bizType, String bizId) {
        FileMetadataDTO dto = fileManageApi.upload(content, originalName, contentType, tenantId, bizType, bizId);
        return dto.getId();
    }

    /** 通过 scm-file 生成预签名下载/预览 URL */
    public Optional<String> presignedUrl(String fileRef, String tenantId) {
        return resolveStorage(fileRef, tenantId)
                .map(meta -> fileQueryApi.generatePresignedUrl(meta.getStorageKey(), tenantId));
    }
}
