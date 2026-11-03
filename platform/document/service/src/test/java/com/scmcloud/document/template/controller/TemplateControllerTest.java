package com.scmcloud.document.template.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.document.template.domain.dto.RenderRequest;
import com.scmcloud.document.template.domain.dto.RenderResult;
import com.scmcloud.document.template.domain.entity.DocTemplate;
import com.scmcloud.document.template.domain.entity.DocTemplateSchema;
import com.scmcloud.document.template.domain.entity.DocTemplateVersion;
import com.scmcloud.document.template.service.TemplateRenderService;
import com.scmcloud.document.template.service.TemplateSchemaService;
import com.scmcloud.document.template.service.TemplateService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateControllerTest {

    @Mock
    private TemplateService templateService;

    @Mock
    private TemplateSchemaService schemaService;

    @Mock
    private TemplateRenderService renderService;

    @InjectMocks
    private TemplateController controller;

    @Test
    void getByIdShouldReturnEmptyPayloadWhenNotInCurrentTenant() {
        when(templateService.findCurrentTenant("tpl-1")).thenReturn(Optional.empty());

        ApiResponse<DocTemplate> response = controller.getById("tpl-1");

        assertEquals(200, response.code());
        assertNull(response.data());
    }

    @Test
    void pageShouldDelegateWithDefaults() {
        Page<DocTemplate> page = new Page<>(1, 20, 0);
        when(templateService.pageQuery(1, 20, null, null, null, null)).thenReturn(page);

        assertSame(page, controller.page(null, null, null, null, null, null).data());
    }

    @Test
    void listVersionsShouldDelegateToService() {
        when(templateService.listVersions("tpl-1")).thenReturn(List.of(new DocTemplateVersion()));

        assertEquals(1, controller.listVersions("tpl-1").data().size());
    }

    @Test
    void updateAndVersionWritesShouldDelegate() {
        DocTemplate template = new DocTemplate();
        DocTemplateVersion version = new DocTemplateVersion();
        when(templateService.updateTemplate("tpl-1", template)).thenReturn(template);
        when(templateService.createVersion("tpl-1", version)).thenReturn(version);
        when(templateService.publishVersion("tpl-1", "ver-1")).thenReturn(version);

        assertSame(template, controller.update("tpl-1", template).data());
        assertSame(version, controller.createVersion("tpl-1", version).data());
        assertSame(version, controller.publishVersion("tpl-1", "ver-1").data());
    }

    @Test
    void schemaAndRenderShouldDelegate() {
        DocTemplateSchema schema = new DocTemplateSchema();
        when(schemaService.saveSchema("tpl-1", schema)).thenReturn(schema);
        when(renderService.render(any())).thenReturn(new RenderResult());

        assertSame(schema, controller.saveSchema("tpl-1", schema).data());
        assertEquals(200, controller.render(new RenderRequest()).code());
        verify(renderService).render(any());
    }
}
