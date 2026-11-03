package com.cognora.platform.document.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.document.file.domain.entity.DocumentFile;
import com.cognora.platform.document.file.mapper.DocumentFileMapper;
import com.cognora.platform.file.api.FileManageApi;
import com.cognora.platform.file.api.FileQueryApi;
import com.cognora.platform.file.api.dto.FileMetadataDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
     * Web 入口：按当前租户上下文查询文档文件，租户由 TenantFilter 统一注入。
     */
    public Optional<DocumentFile> findCurrentTenant(String id) {
        return findById(id, currentTenantId());
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

    /**
     * Web 入口：分页查询当前租户的文档文件。
     */
    public Page<DocumentFile> pageCurrentTenant(int page, int size, String keyword,
                                                String sourceType, String refId) {
        return pageQuery(page, size, currentTenantId(), keyword, sourceType, refId);
    }

    /**
     * Web 入口：登记文档文件元数据，租户取自当前上下文，避免请求体伪造租户。
     */
    @Transactional
    public DocumentFile createCurrentTenant(DocumentFile file) {
        file.setTenantId(currentTenantId());
        save(file);
        return file;
    }

    /**
     * Web 入口：删除文档文件。记录必须属于当前租户，跨租户或不存在时不做删除。
     */
    @Transactional
    public boolean deleteCurrentTenant(String id) {
        Optional<DocumentFile> existing = findCurrentTenant(id);
        if (existing.isEmpty()) {
            return false;
        }
        return removeById(existing.get().getId());
    }

    /**
     * Web 入口：为当前租户的文档文件生成预签名访问地址。
     */
    public Optional<String> presignedUrlCurrentTenant(String fileRef) {
        return presignedUrl(fileRef, currentTenantId());
    }

    /** 通过 platform-file 解析真实文件元数据 */
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

    private String currentTenantId() {
        return TenantContextHolder.getRequiredTenantId().toString();
    }
}