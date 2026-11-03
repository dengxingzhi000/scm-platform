package com.cognora.platform.document.document.service;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.document.document.domain.entity.DocDocument;
import com.cognora.platform.document.document.domain.entity.DocDocumentAudit;
import com.cognora.platform.document.document.mapper.DocDocumentAuditMapper;
import com.cognora.platform.document.document.mapper.DocDocumentMapper;
import com.cognora.platform.document.document.mapper.DocDocumentVersionMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    @Mock
    private DocDocumentMapper documentMapper;

    @Mock
    private DocDocumentVersionMapper versionMapper;

    @Mock
    private DocDocumentAuditMapper auditMapper;

    private DocumentService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, DocDocument.class);
        service = new DocumentService(versionMapper, auditMapper);
        ReflectionTestUtils.setField(service, "baseMapper", documentMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private DocDocument document(String status) {
        DocDocument document = new DocDocument();
        document.setId("doc-1");
        document.setStatus(status);
        document.setCurrentVersion(2);
        document.setTenantId(TENANT_ID.toString());
        return document;
    }

    @Test
    void changeStatusShouldUpdateDocumentAndAppendAudit() {
        DocDocument document = document("GENERATED");
        DocDocument signed = document("SIGNED");
        when(documentMapper.selectById("doc-1")).thenReturn(document, signed);
        when(documentMapper.update(isNull(), any())).thenReturn(1);

        DocDocument result = service.changeStatus("doc-1", "SIGNED", "user-1", "张三");

        assertEquals("SIGNED", result.getStatus());
        ArgumentCaptor<DocDocumentAudit> captor = ArgumentCaptor.forClass(DocDocumentAudit.class);
        verify(auditMapper).insert(captor.capture());
        DocDocumentAudit audit = captor.getValue();
        assertEquals("SIGN", audit.getOperation());
        assertEquals("user-1", audit.getOperatorId());
        assertEquals("张三", audit.getOperatorName());
        assertEquals(2, audit.getBeforeVersion());
        assertEquals(2, audit.getAfterVersion());
        assertEquals(TENANT_ID.toString(), audit.getTenantId());
    }

    @Test
    void changeStatusShouldArchiveSignedDocument() {
        when(documentMapper.selectById("doc-1")).thenReturn(document("SIGNED"), document("ARCHIVED"));
        when(documentMapper.update(isNull(), any())).thenReturn(1);

        service.changeStatus("doc-1", "ARCHIVED", null, null);

        ArgumentCaptor<DocDocumentAudit> captor = ArgumentCaptor.forClass(DocDocumentAudit.class);
        verify(auditMapper).insert(captor.capture());
        assertEquals("ARCHIVE", captor.getValue().getOperation());
    }

    @Test
    void changeStatusShouldRejectIllegalTransition() {
        when(documentMapper.selectById("doc-1")).thenReturn(document("ARCHIVED"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus("doc-1", "SIGNED", "user-1", "张三"));

        assertEquals("illegal document status transition: ARCHIVED -> SIGNED", error.getMessage());
        verify(auditMapper, never()).insert(any(DocDocumentAudit.class));
        verify(documentMapper, never()).update(any(), any());
    }

    @Test
    void changeStatusShouldRejectMissingDocument() {
        when(documentMapper.selectById("doc-1")).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus("doc-1", "SIGNED", null, null));

        assertEquals("document not found: doc-1", error.getMessage());
    }

    @Test
    void changeStatusShouldRejectDocumentFromAnotherTenant() {
        DocDocument foreign = document("GENERATED");
        foreign.setTenantId("019a0aee-0000-7000-8000-0000000000ff");
        when(documentMapper.selectById("doc-1")).thenReturn(foreign);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus("doc-1", "SIGNED", "user-1", "张三"));

        assertEquals("document not found: doc-1", error.getMessage());
        verify(auditMapper, never()).insert(any(DocDocumentAudit.class));
        verify(documentMapper, never()).update(any(), any());
    }
}
