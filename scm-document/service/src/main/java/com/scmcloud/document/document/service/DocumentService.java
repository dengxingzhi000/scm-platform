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
     * 分页查询文档，支持按名称/编码关键字、业务类型和状态过滤。
     */
    public Page<DocDocument> pageQuery(int page, int size, String keyword,
                                       String businessType, String status) {
        LambdaQueryWrapper<DocDocument> wrapper = new LambdaQueryWrapper<>();
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

    public List<DocDocumentVersion> listVersions(String documentId) {
        return versionMapper.selectList(new LambdaQueryWrapper<DocDocumentVersion>()
                .eq(DocDocumentVersion::getDocumentId, documentId)
                .orderByDesc(DocDocumentVersion::getVersion));
    }

    public List<DocDocumentAudit> listAudits(String documentId) {
        return auditMapper.selectList(new LambdaQueryWrapper<DocDocumentAudit>()
                .eq(DocDocumentAudit::getDocumentId, documentId)
                .orderByDesc(DocDocumentAudit::getCreateTime));
    }

    /**
     * 文档状态流转（签署/归档），同时写入审计轨迹。
     */
    @Transactional
    public DocDocument changeStatus(String documentId, String targetStatus,
                                    String operatorId, String operatorName) {
        DocDocument document = getById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("document not found: " + documentId);
        }
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
        audit.setTenantId(TenantContextHolder.getRequiredTenantId().toString());
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
}
