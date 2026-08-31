package com.scmcloud.document.template.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.document.template.domain.dto.RenderRequest;
import com.scmcloud.document.template.domain.dto.RenderResult;
import com.scmcloud.document.template.domain.entity.DocTemplate;
import com.scmcloud.document.template.domain.entity.DocTemplateSchema;
import com.scmcloud.document.template.service.TemplateRenderService;
import com.scmcloud.document.template.service.TemplateSchemaService;
import com.scmcloud.document.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final TemplateSchemaService schemaService;
    private final TemplateRenderService renderService;

    @GetMapping("/{id}")
    public ApiResponse<DocTemplate> getById(@PathVariable String id) {
        return ApiResponse.success(templateService.getById(id));
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<?>> listVersions(@PathVariable String id) {
        return ApiResponse.success(templateService.listVersions(id));
    }

    @PostMapping
    public ApiResponse<DocTemplate> create(@RequestBody DocTemplate template) {
        templateService.save(template);
        return ApiResponse.success(template);
    }

    // ---- 变量 Schema ----

    @PostMapping("/{templateId}/schema")
    public ApiResponse<DocTemplateSchema> saveSchema(@PathVariable String templateId,
                                                     @RequestBody DocTemplateSchema schema) {
        schema.setTemplateId(templateId);
        schemaService.save(schema);
        return ApiResponse.success(schema);
    }

    @GetMapping("/{templateId}/schema")
    public ApiResponse<List<DocTemplateSchema>> listSchema(@PathVariable String templateId) {
        return ApiResponse.success(schemaService.listByTemplate(templateId));
    }

    // ---- 渲染入口 ----

    @PostMapping("/render")
    public ApiResponse<RenderResult> render(@RequestBody RenderRequest request) {
        return ApiResponse.success(renderService.render(request));
    }
}
