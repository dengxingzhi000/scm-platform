package com.scmcloud.file.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.file.api.dto.FileMetadataDTO;
import com.scmcloud.file.convert.FileMetadataConvert;
import com.scmcloud.file.service.download.FileDownloadService;
import com.scmcloud.file.service.upload.FileValidationException;
import com.scmcloud.file.service.upload.InstantUploadService;
import com.scmcloud.file.service.upload.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "文件管理", description = "文件上传、秒传校验、访问地址生成与下载接口。")
public class FileController {

    private final UploadService uploadService;
    private final InstantUploadService instantUploadService;
    private final FileDownloadService fileDownloadService;
    private final FileMetadataConvert fileMetadataConvert;

    @Operation(summary = "上传文件", description = "上传文件并返回文件的元数据信息。")
    @PostMapping("/upload")
    public ApiResponse<FileMetadataDTO> upload(@RequestParam("file") MultipartFile file,
                                                @Parameter(description = "业务类型") @RequestParam(value = "bizType", required = false) String bizType,
                                                @Parameter(description = "业务 ID") @RequestParam(value = "bizId", required = false) String bizId) {
        try {
            String tenantId = TenantContextHolder.getRequiredTenantId().toString();
            FileMetadataDTO metadata = fileMetadataConvert.toDTO(uploadService.upload(
                    file,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    bizType,
                    bizId,
                    tenantId
            ));
            return ApiResponse.success(metadata);
        } catch (FileValidationException e) {
            return ApiResponse.fail(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail(500, "Upload failed: " + e.getMessage());
        }
    }

    @Operation(summary = "校验文件是否存在", description = "根据文件 MD5 校验当前租户下文件是否已存在，用于秒传。")
    @GetMapping("/check/{md5}")
    public ApiResponse<FileMetadataDTO> checkExist(@Parameter(description = "文件 MD5 值") @PathVariable String md5) {
        try {
            String tenantId = TenantContextHolder.getRequiredTenantId().toString();
            Optional<FileMetadataDTO> existing = instantUploadService.checkExist(md5, tenantId)
                    .map(fileMetadataConvert::toDTO);
            return existing
                    .<ApiResponse<FileMetadataDTO>>map(ApiResponse::success)
                    .orElseGet(ApiResponse::success);
        } catch (Exception e) {
            return ApiResponse.fail(500, "Check failed: " + e.getMessage());
        }
    }

    @Operation(summary = "获取文件访问地址", description = "根据文件 ID 生成临时预签名访问地址。")
    @GetMapping("/{id}/url")
    public ApiResponse<Map<String, String>> presignedUrl(@Parameter(description = "文件 ID") @PathVariable String id) {
        try {
            String tenantId = TenantContextHolder.getRequiredTenantId().toString();
            String url = fileDownloadService.generatePresignedUrl(id, tenantId);
            return ApiResponse.success(Map.of("url", url));
        } catch (FileDownloadService.FileNotFoundException e) {
            return ApiResponse.fail(404, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail(500, "Failed to generate url: " + e.getMessage());
        }
    }

    @Operation(summary = "下载文件", description = "根据文件 ID 输出文件内容流供浏览器下载。")
    @GetMapping("/{id}/content")
    public void download(@Parameter(description = "文件 ID") @PathVariable String id, HttpServletResponse response) {
        try {
            String tenantId = TenantContextHolder.getRequiredTenantId().toString();
            FileMetadataDTO metadata = fileMetadataConvert.toDTO(fileDownloadService.resolve(id, tenantId));
            try (var content = fileDownloadService.openContent(id, tenantId)) {
                response.setContentType(metadata.getContentType());
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(encode(metadata.getOriginalName()))
                                .build().toString());
                if (metadata.getFileSize() != null) {
                    response.setContentLengthLong(metadata.getFileSize());
                }
                content.transferTo(response.getOutputStream());
            }
        } catch (FileDownloadService.FileNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String encode(String name) {
        if (name == null) {
            return "file";
        }
        return URLEncoder.encode(name, StandardCharsets.UTF_8);
    }
}
