package com.scmcloud.document.document.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.document.document.domain.dto.DocumentStatusChangeRequest;
import com.scmcloud.document.document.domain.entity.DocDocument;
import com.scmcloud.document.document.domain.entity.DocDocumentAudit;
import com.scmcloud.document.document.domain.entity.DocDocumentVersion;
import com.scmcloud.document.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "文档管理", description = "文档的分页查询、详情与创建接口。")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "分页查询文档", description = "按名称/编码关键字、业务类型和状态分页查询文档。")
    @GetMapping("/page")
    public ApiResponse<Page<DocDocument>> page(
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量，默认 20") @RequestParam(required = false) Integer size,
            @Parameter(description = "名称或编码关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "业务类型") @RequestParam(required = false) String businessType,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        return ApiResponse.success(documentService.pageQuery(
                page == null ? 1 : page, size == null ? 20 : size, keyword, businessType, status));
    }

    @Operation(summary = "查询文档", description = "根据文档 ID 查询当前租户的文档详情。")
    @GetMapping("/{id}")
    public ApiResponse<DocDocument> getById(@Parameter(description = "文档 ID") @PathVariable String id) {
        return documentService.findCurrentTenant(id)
                .<ApiResponse<DocDocument>>map(ApiResponse::success)
                .orElseGet(ApiResponse::success);
    }

    @Operation(summary = "创建文档", description = "手工登记一份文档，初始状态为已生成。")
    @PostMapping
    public ApiResponse<DocDocument> create(@RequestBody DocDocument document) {
        return ApiResponse.success(documentService.createDocument(document));
    }

    @Operation(summary = "查询文档版本", description = "根据文档 ID 查询全部渲染版本，按版本号倒序。")
    @GetMapping("/{id}/versions")
    public ApiResponse<List<DocDocumentVersion>> listVersions(@Parameter(description = "文档 ID") @PathVariable String id) {
        return ApiResponse.success(documentService.listVersions(id));
    }

    @Operation(summary = "查询文档审计轨迹", description = "根据文档 ID 查询生成、渲染、签署、归档等操作记录。")
    @GetMapping("/{id}/audits")
    public ApiResponse<List<DocDocumentAudit>> listAudits(@Parameter(description = "文档 ID") @PathVariable String id) {
        return ApiResponse.success(documentService.listAudits(id));
    }

    @Operation(summary = "变更文档状态", description = "签署（SIGNED）或归档（ARCHIVED），并写入审计轨迹。")
    @PostMapping("/{id}/status")
    public ApiResponse<DocDocument> changeStatus(@Parameter(description = "文档 ID") @PathVariable String id,
                                                 @RequestBody DocumentStatusChangeRequest request) {
        return ApiResponse.success(documentService.changeStatus(
                id, request.getStatus(), request.getOperatorId(), request.getOperatorName()));
    }
}
