package com.scmcloud.document.document.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.document.document.domain.entity.DocDocument;
import com.scmcloud.document.document.domain.entity.DocDocumentAudit;
import com.scmcloud.document.document.domain.entity.DocDocumentVersion;
import com.scmcloud.document.document.mapper.DocDocumentMapper;
import com.scmcloud.document.document.mapper.DocDocumentVersionMapper;
import com.scmcloud.document.document.mapper.DocDocumentAuditMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DocumentService extends ServiceImpl<DocDocumentMapper, DocDocument> {

    private final DocDocumentVersionMapper versionMapper;
    private final DocDocumentAuditMapper auditMapper;

    public DocumentService(DocDocumentVersionMapper versionMapper, DocDocumentAuditMapper auditMapper) {
        this.versionMapper = versionMapper;
        this.auditMapper = auditMapper;
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
