package com.scmcloud.document.file.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.document.file.domain.entity.DocumentFile;
import com.scmcloud.document.file.service.DocumentFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/document-files")
@RequiredArgsConstructor
public class DocumentFileController {

    private final DocumentFileService documentFileService;

    @GetMapping("/{id}")
    public ApiResponse<DocumentFile> getById(@PathVariable String id) {
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        return documentFileService.findById(id, tenantId)
                .<ApiResponse<DocumentFile>>map(ApiResponse::success)
                .orElseGet(ApiResponse::success);
    }

    @GetMapping("/{id}/url")
    public ApiResponse<Map<String, String>> presignedUrl(@PathVariable String id) {
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        Optional<DocumentFile> file = documentFileService.findById(id, tenantId);
        if (file.isEmpty()) {
            return ApiResponse.fail(404, "document file not found");
        }
        return documentFileService.presignedUrl(file.get().getFileRef(), tenantId)
                .map(url -> ApiResponse.success(Map.of("url", url)))
                .orElseGet(() -> ApiResponse.fail(404, "storage file not found"));
    }

    @PostMapping
    public ApiResponse<DocumentFile> create(@RequestBody DocumentFile file) {
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        file.setTenantId(tenantId);
        documentFileService.save(file);
        return ApiResponse.success(file);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        documentFileService.removeById(id);
        return ApiResponse.success();
    }
}
