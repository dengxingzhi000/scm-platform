package com.scmcloud.document.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.document.document.domain.entity.DocDocument;
import com.scmcloud.document.document.domain.entity.DocDocumentAudit;
import com.scmcloud.document.document.domain.entity.DocDocumentVersion;
import com.scmcloud.document.document.mapper.DocDocumentAuditMapper;
import com.scmcloud.document.document.mapper.DocDocumentMapper;
import com.scmcloud.document.document.mapper.DocDocumentVersionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class DocumentService extends ServiceImpl<DocDocumentMapper, DocDocument> {

    /** 文档生命周期状态流转：GENERATED 可签署/归档，SIGNED 仅可归档。 */
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "GENERATED", Set.of("SIGNED", "ARCHIVED"),
            "SIGNED", Set.of("ARCHIVED"));

    private final DocDocumentVersionMapper versionMapper;
    private final DocDocumentAuditMapper auditMapper;

    public DocumentService(DocDocumentVersionMapper versionMapper, DocDocumentAuditMapper auditMapper) {
        this.versionMapper = versionMapper;
        this.auditMapper = auditMapper;
    }

    /**
     * 分页查询当前租户的文档，支持按名称/编码关键字、业务类型和状态过滤。
     */
    public Page<DocDocument> pageQuery(int page, int size, String keyword,
                                       String businessType, String status) {
        LambdaQueryWrapper<DocDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocDocument::getTenantId, currentTenantId());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(DocDocument::getDocName, keyword)
                    .or().like(DocDocument::getDocCode, keyword));
        }
        wrapper.eq(StringUtils.hasText(businessType), DocDocument::getBusinessType, businessType)
                .eq(StringUtils.hasText(status), DocDocument::getStatus, status)
                .orderByDesc(DocDocument::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    /**
     * 手工创建文档。租户来自当前上下文，避免绕过租户隔离。
     */
    @Transactional
    public DocDocument createDocument(DocDocument document) {
        document.setTenantId(TenantContextHolder.getRequiredTenantId().toString());
        if (!StringUtils.hasText(document.getStatus())) {
            document.setStatus("GENERATED");
        }
        if (document.getCurrentVersion() == null) {
            document.setCurrentVersion(1);
        }
        save(document);
        return document;
    }

    /**
     * 按 ID 查询当前租户的文档；不存在或属于其他租户时返回空。
     */
    public Optional<DocDocument> findCurrentTenant(String id) {
        DocDocument document = getById(id);
        if (document == null || !currentTenantId().equals(document.getTenantId())) {
            return Optional.empty();
        }
        return Optional.of(document);
    }

    /**
     * 查询当前租户文档的版本列表；文档不存在或不属于当前租户时返回空列表。
     */
    public List<DocDocumentVersion> listVersions(String documentId) {
        if (findCurrentTenant(documentId).isEmpty()) {
            return List.of();
        }
        return versionMapper.selectList(new LambdaQueryWrapper<DocDocumentVersion>()
                .eq(DocDocumentVersion::getDocumentId, documentId)
                .eq(DocDocumentVersion::getTenantId, currentTenantId())
                .orderByDesc(DocDocumentVersion::getVersion));
    }

    /**
     * 查询当前租户文档的审计轨迹；文档不存在或不属于当前租户时返回空列表。
     */
    public List<DocDocumentAudit> listAudits(String documentId) {
        if (findCurrentTenant(documentId).isEmpty()) {
            return List.of();
        }
        return auditMapper.selectList(new LambdaQueryWrapper<DocDocumentAudit>()
                .eq(DocDocumentAudit::getDocumentId, documentId)
                .eq(DocDocumentAudit::getTenantId, currentTenantId())
                .orderByDesc(DocDocumentAudit::getCreateTime));
    }

    /**
     * 文档状态流转（签署/归档），同时写入审计轨迹。
     */
    @Transactional
    public DocDocument changeStatus(String documentId, String targetStatus,
                                    String operatorId, String operatorName) {
        DocDocument document = findCurrentTenant(documentId)
                .orElseThrow(() -> new IllegalArgumentException("document not found: " + documentId));
        String currentStatus = StringUtils.hasText(document.getStatus()) ? document.getStatus() : "GENERATED";
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new IllegalArgumentException(
                    "illegal document status transition: " + currentStatus + " -> " + targetStatus);
        }

        Integer beforeVersion = document.getCurrentVersion() == null ? 0 : document.getCurrentVersion();
        lambdaUpdate()
                .eq(DocDocument::getId, documentId)
                .set(DocDocument::getStatus, targetStatus)
                .update();

        DocDocumentAudit audit = new DocDocumentAudit();
        audit.setDocumentId(documentId);
        audit.setOperation("SIGNED".equals(targetStatus) ? "SIGN" : "ARCHIVE");
        audit.setOperatorId(operatorId);
        audit.setOperatorName(operatorName);
        audit.setBeforeVersion(beforeVersion);
        audit.setAfterVersion(beforeVersion);
        audit.setTenantId(currentTenantId());
        audit.setCreateTime(new Date());
        auditMapper.insert(audit);

        return getById(documentId);
    }

    @Transactional
    public DocDocumentVersion addVersion(DocDocument document, DocDocumentVersion version, DocDocumentAudit audit) {
        versionMapper.insert(version);
        auditMapper.insert(audit);
        lambdaUpdate()
                .eq(DocDocument::getId, document.getId())
                .set(DocDocument::getCurrentVersion, version.getVersion())
                .update();
        return version;
    }

    private String currentTenantId() {
        return TenantContextHolder.getRequiredTenantId().toString();
    }
}
