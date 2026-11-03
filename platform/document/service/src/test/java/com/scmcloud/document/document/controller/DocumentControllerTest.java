package com.scmcloud.document.document.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.document.document.domain.dto.DocumentStatusChangeRequest;
import com.scmcloud.document.document.domain.entity.DocDocument;
import com.scmcloud.document.document.domain.entity.DocDocumentAudit;
import com.scmcloud.document.document.domain.entity.DocDocumentVersion;
import com.scmcloud.document.document.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentController controller;

    @Test
    void pageShouldUseDefaultPagingAndDelegate() {
        Page<DocDocument> page = new Page<>(1, 20, 0);
        when(documentService.pageQuery(1, 20, "contract", "CONTRACT", "GENERATED")).thenReturn(page);

        ApiResponse<Page<DocDocument>> response = controller.page(null, null, "contract", "CONTRACT", "GENERATED");

        assertEquals(200, response.code());
        assertSame(page, response.data());
    }

    @Test
    void getByIdShouldReturnEmptyPayloadWhenNotInCurrentTenant() {
        when(documentService.findCurrentTenant("doc-1")).thenReturn(Optional.empty());

        ApiResponse<DocDocument> response = controller.getById("doc-1");

        assertEquals(200, response.code());
        assertNull(response.data());
    }

    @Test
    void getByIdShouldReturnTheTenantScopedDocument() {
        DocDocument document = new DocDocument();
        document.setId("doc-1");
        when(documentService.findCurrentTenant("doc-1")).thenReturn(Optional.of(document));

        ApiResponse<DocDocument> response = controller.getById("doc-1");

        assertSame(document, response.data());
    }

    @Test
    void createShouldDelegateToService() {
        DocDocument document = new DocDocument();
        when(documentService.createDocument(document)).thenReturn(document);

        assertSame(document, controller.create(document).data());
    }

    @Test
    void versionsAndAuditsShouldDelegate() {
        DocDocumentVersion version = new DocDocumentVersion();
        DocDocumentAudit audit = new DocDocumentAudit();
        when(documentService.listVersions("doc-1")).thenReturn(List.of(version));
        when(documentService.listAudits("doc-1")).thenReturn(List.of(audit));

        assertEquals(1, controller.listVersions("doc-1").data().size());
        assertEquals(1, controller.listAudits("doc-1").data().size());
    }

    @Test
    void changeStatusShouldPassOperatorFieldsToService() {
        DocumentStatusChangeRequest request = new DocumentStatusChangeRequest();
        request.setStatus("SIGNED");
        request.setOperatorId("user-1");
        request.setOperatorName("张三");
        DocDocument signed = new DocDocument();
        signed.setStatus("SIGNED");
        when(documentService.changeStatus("doc-1", "SIGNED", "user-1", "张三")).thenReturn(signed);

        ApiResponse<DocDocument> response = controller.changeStatus("doc-1", request);

        assertSame(signed, response.data());
        verify(documentService).changeStatus("doc-1", "SIGNED", "user-1", "张三");
    }
}
