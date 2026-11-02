package com.scmcloud.document.template.service;

import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.document.template.domain.entity.DocTemplate;
import com.scmcloud.document.template.domain.entity.DocTemplateVersion;
import com.scmcloud.document.template.mapper.DocTemplateMapper;
import com.scmcloud.document.template.mapper.DocTemplateVersionMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    @Mock
    private DocTemplateMapper templateMapper;

    @Mock
    private DocTemplateVersionMapper versionMapper;

    private TemplateService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, DocTemplate.class);
        TableInfoHelper.initTableInfo(assistant, DocTemplateVersion.class);
        service = new TemplateService(versionMapper);
        ReflectionTestUtils.setField(service, "baseMapper", templateMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private DocTemplate template() {
        DocTemplate template = new DocTemplate();
        template.setId("tpl-1");
        template.setTenantId(TENANT_ID.toString());
        template.setStatus("DRAFT");
        return template;
    }

    @Test
    void createVersionShouldAssignNextVersionAndTenant() {
        when(templateMapper.selectById("tpl-1")).thenReturn(template());
        DocTemplateVersion latest = new DocTemplateVersion();
        latest.setVersion(3);
        when(versionMapper.selectOne(any())).thenReturn(latest);

        DocTemplateVersion request = new DocTemplateVersion();
        request.setFileId("file-1");
        request.setSchemaId("schema-1");

        DocTemplateVersion created = service.createVersion("tpl-1", request);

        assertEquals(4, created.getVersion());
        assertEquals("DRAFT", created.getStatus());
        assertEquals("tpl-1", created.getTemplateId());
        assertEquals("file-1", created.getFileId());
        assertEquals(TENANT_ID.toString(), created.getTenantId());
        assertNotNull(created.getCreateTime());
        verify(versionMapper).insert(created);
    }

    @Test
    void createVersionShouldStartAtOneWhenNoHistory() {
        when(templateMapper.selectById("tpl-1")).thenReturn(template());
        when(versionMapper.selectOne(any())).thenReturn(null);

        DocTemplateVersion created = service.createVersion("tpl-1", new DocTemplateVersion());

        assertEquals(1, created.getVersion());
    }

    @Test
    void createVersionShouldRejectUnknownTemplate() {
        when(templateMapper.selectById("tpl-missing")).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createVersion("tpl-missing", new DocTemplateVersion()));

        assertEquals("document template not found: tpl-missing", error.getMessage());
        verify(versionMapper, never()).insert(any(DocTemplateVersion.class));
    }

    private DocTemplateVersion version(String templateId) {
        DocTemplateVersion version = new DocTemplateVersion();
        version.setId("ver-1");
        version.setTemplateId(templateId);
        version.setVersion(2);
        version.setTenantId(TENANT_ID.toString());
        version.setStatus("DRAFT");
        return version;
    }

    @Test
    void publishVersionShouldArchivePreviousAndSwitchTemplate() {
        DocTemplateVersion version = version("tpl-1");
        version.setFileId("file-1");
        when(templateMapper.selectById("tpl-1")).thenReturn(template());
        when(versionMapper.selectById("ver-1")).thenReturn(version);
        when(versionMapper.updateById(version)).thenReturn(1);
        when(templateMapper.update(any(), any())).thenReturn(1);

        DocTemplateVersion published = service.publishVersion("tpl-1", "ver-1");

        assertEquals("PUBLISHED", published.getStatus());
        assertNotNull(published.getPublishedAt());
        verify(versionMapper).update(isNull(), any());
        verify(templateMapper).update(isNull(), any());
    }

    @Test
    void publishVersionShouldRejectVersionWithoutSourceFile() {
        when(templateMapper.selectById("tpl-1")).thenReturn(template());
        when(versionMapper.selectById("ver-1")).thenReturn(version("tpl-1"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.publishVersion("tpl-1", "ver-1"));

        assertEquals("template version has no source file, upload the template file first", error.getMessage());
        verify(versionMapper, never()).updateById(any(DocTemplateVersion.class));
    }

    @Test
    void publishVersionShouldRejectVersionFromAnotherTemplate() {
        DocTemplateVersion version = version("tpl-2");
        version.setFileId("file-1");
        when(templateMapper.selectById("tpl-1")).thenReturn(template());
        when(versionMapper.selectById("ver-1")).thenReturn(version);

        assertThrows(IllegalArgumentException.class, () -> service.publishVersion("tpl-1", "ver-1"));
        verify(versionMapper, never()).updateById(any(DocTemplateVersion.class));
    }

    @Test
    void publishVersionShouldRejectVersionFromAnotherTenant() {
        DocTemplateVersion version = version("tpl-1");
        version.setFileId("file-1");
        version.setTenantId("019a0aee-0000-7000-8000-0000000000ff");
        when(templateMapper.selectById("tpl-1")).thenReturn(template());
        when(versionMapper.selectById("ver-1")).thenReturn(version);

        assertThrows(IllegalArgumentException.class, () -> service.publishVersion("tpl-1", "ver-1"));
        verify(versionMapper, never()).updateById(any(DocTemplateVersion.class));
    }

    @Test
    void listVersionsShouldReturnEmptyForUnknownTemplate() {
        when(templateMapper.selectById("tpl-missing")).thenReturn(null);

        assertTrue(service.listVersions("tpl-missing").isEmpty());
        verify(versionMapper, never()).selectList(any());
    }
}
