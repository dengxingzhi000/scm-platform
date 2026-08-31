package com.scmcloud.document.template.service;

import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.document.document.domain.entity.DocDocument;
import com.scmcloud.document.document.domain.entity.DocDocumentAudit;
import com.scmcloud.document.document.domain.entity.DocDocumentVersion;
import com.scmcloud.document.document.service.DocumentService;
import com.scmcloud.document.file.domain.entity.DocumentFile;
import com.scmcloud.document.file.service.DocumentFileService;
import com.scmcloud.document.template.domain.dto.RenderRequest;
import com.scmcloud.document.template.domain.dto.RenderResult;
import com.scmcloud.document.template.domain.dto.TemplateVariable;
import com.scmcloud.document.template.domain.entity.DocTemplate;
import com.scmcloud.document.template.domain.entity.DocTemplateSchema;
import com.scmcloud.document.template.domain.entity.DocTemplateVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TemplateRenderService {

    private final TemplateService templateService;
    private final TemplateSchemaService schemaService;
    private final DocumentService documentService;
    private final DocumentFileService documentFileService;
    private final TemplateEngine templateEngine;

    public TemplateRenderService(TemplateService templateService,
                                 TemplateSchemaService schemaService,
                                 DocumentService documentService,
                                 DocumentFileService documentFileService,
                                 TemplateEngine templateEngine) {
        this.templateService = templateService;
        this.schemaService = schemaService;
        this.documentService = documentService;
        this.documentFileService = documentFileService;
        this.templateEngine = templateEngine;
    }

    @Transactional
    public RenderResult render(RenderRequest request) {
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();

        DocTemplateVersion version = templateService.getVersion(request.getTemplateVersionId());
        if (version == null) {
            throw new IllegalArgumentException("template version not found: " + request.getTemplateVersionId());
        }
        DocTemplate template = templateService.getById(version.getTemplateId());

        // 1. 加载变量 schema 并校验必填项
        List<TemplateVariable> variables = List.of();
        if (version.getSchemaId() != null) {
            DocTemplateSchema schema = schemaService.getById(version.getSchemaId());
            if (schema != null) {
                variables = schema.parseVariables();
            }
        }
        validate(variables, request.getData());

        // 2. 取模板源文件字节 + 引擎渲染 + 上传产物(字节始终存于 scm-file)
        if (version.getFileId() == null) {
            throw new IllegalStateException(
                    "template version has no source file: upload template source and set version.fileId first");
        }
        DocumentFile srcCatalog = documentFileService.findById(version.getFileId(), tenantId)
                .orElseThrow(() -> new IllegalStateException("template source file catalog missing"));
        byte[] templateBytes = documentFileService.downloadStorage(srcCatalog.getFileRef(), tenantId);
        if (templateBytes == null) {
            throw new IllegalStateException("template source bytes not resolvable from scm-file");
        }
        byte[] output = templateEngine.render(templateBytes, request.getData(), variables);

        String outputName = (template != null ? template.getTemplateName() : "document")
                + "-" + version.getVersion() + ".docx";
        String scmFileId = documentFileService.storeOutput(
                output, outputName,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                tenantId, "DOCUMENT", null);

        DocumentFile outCatalog = new DocumentFile();
        outCatalog.setOriginalName(outputName);
        outCatalog.setSourceType("RENDERED_OUTPUT");
        outCatalog.setFileRef(scmFileId);
        outCatalog.setTenantId(tenantId);
        outCatalog.setCreateBy(request.getOperatorId());
        outCatalog.setCreateTime(new Date());

        // 3. 落库文档生命周期: document + version + audit
        DocDocument doc = new DocDocument();
        doc.setDocCode("DOC" + System.currentTimeMillis());
        doc.setDocName(template != null ? template.getTemplateName() : version.getTemplateId());
        doc.setTemplateId(version.getTemplateId());
        doc.setTemplateVersion(version.getVersion());
        doc.setCurrentVersion(1);
        doc.setStatus("GENERATED");
        doc.setBusinessType(request.getBusinessType());
        doc.setBusinessId(request.getBusinessId());
        doc.setTenantId(tenantId);
        doc.setCreatedBy(request.getOperatorId());
        doc.setCreateTime(new Date());
        documentService.save(doc);

        outCatalog.setRefId(doc.getId());
        documentFileService.save(outCatalog);

        DocDocumentVersion dv = new DocDocumentVersion();
        dv.setDocumentId(doc.getId());
        dv.setVersion(1);
        dv.setFileId(outCatalog.getId());
        dv.setRenderParamsChecksum(Integer.toHexString(
                com.alibaba.fastjson2.JSON.toJSONString(request.getData()).hashCode()));
        dv.setTenantId(tenantId);
        dv.setCreateTime(new Date());

        DocDocumentAudit audit = new DocDocumentAudit();
        audit.setDocumentId(doc.getId());
        audit.setOperation("RENDER");
        audit.setOperatorId(request.getOperatorId());
        audit.setOperatorName(request.getOperatorName());
        audit.setAfterVersion(1);
        audit.setTenantId(tenantId);
        audit.setCreateTime(new Date());

        documentService.addVersion(doc, dv, audit);

        RenderResult result = new RenderResult();
        result.setDocumentId(doc.getId());
        result.setDocumentVersion(1);
        result.setDocCode(doc.getDocCode());
        result.setStatus(doc.getStatus());
        return result;
    }

    private void validate(List<TemplateVariable> variables, Map<String, Object> data) {
        if (variables == null || data == null) {
            return;
        }
        for (TemplateVariable v : variables) {
            if (v.isRequired() && (data.get(v.getName()) == null)) {
                throw new IllegalArgumentException("missing required template variable: " + v.getName());
            }
        }
    }
}
