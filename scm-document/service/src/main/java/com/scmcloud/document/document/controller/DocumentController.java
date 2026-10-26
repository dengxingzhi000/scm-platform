package com.scmcloud.document.document.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.document.document.domain.entity.DocDocument;
import com.scmcloud.document.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "文档管理", description = "文档的查询与创建接口。")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "查询文档", description = "根据文档 ID 查询文档详情。")
    @GetMapping("/{id}")
    public ApiResponse<DocDocument> getById(@Parameter(description = "文档 ID") @PathVariable String id) {
        return ApiResponse.success(documentService.getById(id));
    }

    @Operation(summary = "创建文档", description = "新建并保存一份文档。")
    @PostMapping
    public ApiResponse<DocDocument> create(@RequestBody DocDocument document) {
        documentService.save(document);
        return ApiResponse.success(document);
    }
}
