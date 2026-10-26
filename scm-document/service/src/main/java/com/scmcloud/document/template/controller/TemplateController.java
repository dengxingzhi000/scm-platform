package com.scmcloud.document.template.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.document.template.domain.dto.RenderRequest;
import com.scmcloud.document.template.domain.dto.RenderResult;
import com.scmcloud.document.template.domain.entity.DocTemplate;
import com.scmcloud.document.template.domain.entity.DocTemplateSchema;
import com.scmcloud.document.template.service.TemplateRenderService;
import com.scmcloud.document.template.service.TemplateSchemaService;
import com.scmcloud.document.template.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "文档模板", description = "文档模板、变量 Schema 与模板渲染接口。")
public class TemplateController {

    private final TemplateService templateService;
    private final TemplateSchemaService schemaService;
    private final TemplateRenderService renderService;

    @Operation(summary = "查询模板", description = "根据模板 ID 查询文档模板详情。")
    @GetMapping("/{id}")
    public ApiResponse<DocTemplate> getById(@Parameter(description = "模板 ID") @PathVariable String id) {
        return ApiResponse.success(templateService.getById(id));
    }

    @Operation(summary = "查询模板版本", description = "根据模板 ID 查询该模板的历史版本列表。")
    @GetMapping("/{id}/versions")
    public ApiResponse<List<?>> listVersions(@Parameter(description = "模板 ID") @PathVariable String id) {
        return ApiResponse.success(templateService.listVersions(id));
    }

    @Operation(summary = "创建模板", description = "新建并保存一个文档模板。")
    @PostMapping
    public ApiResponse<DocTemplate> create(@RequestBody DocTemplate template) {
        templateService.save(template);
        return ApiResponse.success(template);
    }

    // ---- 变量 Schema ----

    @Operation(summary = "保存模板变量 Schema", description = "为指定模板保存变量 Schema 定义。")
    @PostMapping("/{templateId}/schema")
    public ApiResponse<DocTemplateSchema> saveSchema(@Parameter(description = "模板 ID") @PathVariable String templateId,
                                                     @RequestBody DocTemplateSchema schema) {
        schema.setTemplateId(templateId);
        schemaService.save(schema);
        return ApiResponse.success(schema);
    }

    @Operation(summary = "查询模板变量 Schema", description = "根据模板 ID 查询该模板的全部变量 Schema。")
    @GetMapping("/{templateId}/schema")
    public ApiResponse<List<DocTemplateSchema>> listSchema(@Parameter(description = "模板 ID") @PathVariable String templateId) {
        return ApiResponse.success(schemaService.listByTemplate(templateId));
    }

    // ---- 渲染入口 ----

    @Operation(summary = "渲染模板", description = "按模板和变量数据渲染生成文档。")
    @PostMapping("/render")
    public ApiResponse<RenderResult> render(@RequestBody RenderRequest request) {
        return ApiResponse.success(renderService.render(request));
    }
}
