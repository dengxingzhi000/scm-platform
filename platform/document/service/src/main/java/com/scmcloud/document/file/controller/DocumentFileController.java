package com.scmcloud.document.file.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.document.file.domain.entity.DocumentFile;
import com.scmcloud.document.file.service.DocumentFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/document-files")
@RequiredArgsConstructor
@Tag(name = "文档文件", description = "文档文件元数据、访问地址与删除接口。")
public class DocumentFileController {

    private final DocumentFileService documentFileService;

    @Operation(summary = "分页查询文档文件", description = "按名称关键字、来源类型和关联业务 ID 分页查询当前租户的文档文件。")
    @GetMapping("/page")
    public ApiResponse<Page<DocumentFile>> page(
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量，默认 20") @RequestParam(required = false) Integer size,
            @Parameter(description = "文件名称关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "来源类型：TEMPLATE_SOURCE / RENDERED_OUTPUT") @RequestParam(required = false) String sourceType,
            @Parameter(description = "关联业务 ID") @RequestParam(required = false) String refId) {
        return ApiResponse.success(documentFileService.pageCurrentTenant(
                page == null ? 1 : page, size == null ? 20 : size, keyword, sourceType, refId));
    }

    @Operation(summary = "查询文档文件", description = "根据文件 ID 查询当前租户下的文档文件元数据。")
    @GetMapping("/{id}")
    public ApiResponse<DocumentFile> getById(@Parameter(description = "文档文件 ID") @PathVariable String id) {
        return documentFileService.findCurrentTenant(id)
                .<ApiResponse<DocumentFile>>map(ApiResponse::success)
                .orElseGet(ApiResponse::success);
    }

    @Operation(summary = "获取文件访问地址", description = "根据文件 ID 生成文档文件的临时预签名访问地址。")
    @GetMapping("/{id}/url")
    public ApiResponse<Map<String, String>> presignedUrl(@Parameter(description = "文档文件 ID") @PathVariable String id) {
        Optional<DocumentFile> file = documentFileService.findCurrentTenant(id);
        if (file.isEmpty()) {
            return ApiResponse.fail(404, "document file not found");
        }
        return documentFileService.presignedUrlCurrentTenant(file.get().getFileRef())
                .map(url -> ApiResponse.success(Map.of("url", url)))
                .orElseGet(() -> ApiResponse.fail(404, "storage file not found"));
    }

    @Operation(summary = "创建文档文件", description = "保存文档文件元数据并绑定当前租户。")
    @PostMapping
    public ApiResponse<DocumentFile> create(@RequestBody DocumentFile file) {
        return ApiResponse.success(documentFileService.createCurrentTenant(file));
    }

    @Operation(summary = "删除文档文件", description = "根据文件 ID 删除当前租户的文档文件记录。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "文档文件 ID") @PathVariable String id) {
        documentFileService.deleteCurrentTenant(id);
        return ApiResponse.success();
    }
}
