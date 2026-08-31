package com.scmcloud.document.document.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.document.document.domain.entity.DocDocument;
import com.scmcloud.document.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/{id}")
    public ApiResponse<DocDocument> getById(@PathVariable String id) {
        return ApiResponse.success(documentService.getById(id));
    }

    @PostMapping
    public ApiResponse<DocDocument> create(@RequestBody DocDocument document) {
        documentService.save(document);
        return ApiResponse.success(document);
    }
}
