package com.scmcloud.file.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.file.api.dto.FileBatchDeleteRequest;
import com.scmcloud.file.api.dto.FileMetadataDTO;
import com.scmcloud.file.api.dto.FileStatsDTO;
import com.scmcloud.file.api.dto.FileUpdateRequest;
import com.scmcloud.file.convert.FileMetadataConvert;
import com.scmcloud.file.service.FileNotFoundException;
import com.scmcloud.file.service.download.FileDownloadService;
import com.scmcloud.file.service.management.FileManagementService;
import com.scmcloud.file.service.metadata.FileMetadataService;
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
    private final FileMetadataService fileMetadataService;
    private final FileManagementService fileManagementService;

    @Operation(summary = "分页查询文件", description = "按名称关键字、业务类型和业务 ID 分页查询当前租户的文件元数据。")
    @GetMapping("/page")
    public ApiResponse<Page<FileMetadataDTO>> page(
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量，默认 20") @RequestParam(required = false) Integer size,
            @Parameter(description = "文件名关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "业务类型") @RequestParam(required = false) String bizType,
            @Parameter(description = "业务 ID") @RequestParam(required = false) String bizId) {
        return ApiResponse.success(fileMetadataService.pageFiles(
                page == null ? 1 : page, size == null ? 20 : size, keyword, bizType, bizId));
    }

    @Operation(summary = "文件统计", description = "统计当前租户的文件总数、占用空间、类型分布与最近上传时间。")
    @GetMapping("/stats")
    public ApiResponse<FileStatsDTO> stats() {
        return ApiResponse.success(fileMetadataService.getStats());
    }

    @Operation(summary = "更新文件信息", description = "重命名文件或调整业务类型、业务 ID 关联。传空表示解除关联。")
    @PutMapping("/{id}")
    public ApiResponse<FileMetadataDTO> update(
            @Parameter(description = "文件 ID") @PathVariable String id,
            @RequestBody FileUpdateRequest request) {
        try {
            return ApiResponse.success(fileMetadataConvert.toDTO(fileManagementService.update(
                    id, request.getOriginalName(), request.getBizType(), request.getBizId())));
        } catch (FileNotFoundException e) {
            return ApiResponse.fail(404, e.getMessage());
        } catch (FileValidationException e) {
            return ApiResponse.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update file {}", id, e);
            return ApiResponse.fail(500, "Update failed");
        }
    }

    @Operation(summary = "删除文件", description = "删除文件元数据与存储对象。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "文件 ID") @PathVariable String id) {
        try {
            fileManagementService.delete(id);
            return ApiResponse.success();
        } catch (FileNotFoundException e) {
            return ApiResponse.fail(404, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete file {}", id, e);
            return ApiResponse.fail(500, "Delete failed");
        }
    }

    @Operation(summary = "批量删除文件", description = "按 ID 列表批量删除当前租户的文件，返回实际删除数量。")
    @PostMapping("/batch-delete")
    public ApiResponse<Integer> batchDelete(@RequestBody FileBatchDeleteRequest request) {
        try {
            return ApiResponse.success(fileManagementService.deleteBatch(request.getIds()));
        } catch (Exception e) {
            log.error("Failed to delete files in batch", e);
            return ApiResponse.fail(500, "Batch delete failed");
        }
    }

    @Operation(summary = "上传文件", description = "上传文件并返回文件的元数据信息。")
    @PostMapping("/upload")
    public ApiResponse<FileMetadataDTO> upload(@RequestParam("file") MultipartFile file,
                                                @Parameter(description = "业务类型") @RequestParam(value = "bizType", required = false) String bizType,
                                                @Parameter(description = "业务 ID") @RequestParam(value = "bizId", required = false) String bizId) {
        try {
            FileMetadataDTO metadata = fileMetadataConvert.toDTO(uploadService.upload(
                    file,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    bizType,
                    bizId
            ));
            return ApiResponse.success(metadata);
        } catch (FileValidationException e) {
            return ApiResponse.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to upload file {}", file.getOriginalFilename(), e);
            return ApiResponse.fail(500, "Upload failed");
        }
    }

    @Operation(summary = "校验文件是否存在", description = "根据文件 MD5 校验当前租户下文件是否已存在，用于秒传。")
    @GetMapping("/check/{md5}")
    public ApiResponse<FileMetadataDTO> checkExist(@Parameter(description = "文件 MD5 值") @PathVariable String md5) {
        try {
            Optional<FileMetadataDTO> existing = instantUploadService.checkExist(md5)
                    .map(fileMetadataConvert::toDTO);
            return existing
                    .<ApiResponse<FileMetadataDTO>>map(ApiResponse::success)
                    .orElseGet(ApiResponse::success);
        } catch (Exception e) {
            log.error("Failed to check file existence for md5 {}", md5, e);
            return ApiResponse.fail(500, "Check failed");
        }
    }

    @Operation(summary = "获取文件访问地址", description = "根据文件 ID 生成临时预签名访问地址。")
    @GetMapping("/{id}/url")
    public ApiResponse<Map<String, String>> presignedUrl(@Parameter(description = "文件 ID") @PathVariable String id) {
        try {
            String url = fileDownloadService.generatePresignedUrl(id);
            return ApiResponse.success(Map.of("url", url));
        } catch (FileNotFoundException e) {
            return ApiResponse.fail(404, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to generate presigned url for file {}", id, e);
            return ApiResponse.fail(500, "Failed to generate url");
        }
    }

    @Operation(summary = "预览文件", description = "以 inline 方式输出文件内容，供浏览器直接预览图片、PDF 或文本。")
    @GetMapping("/{id}/preview")
    public void preview(@Parameter(description = "文件 ID") @PathVariable String id, HttpServletResponse response) {
        streamContent(id, response, true);
    }

    @Operation(summary = "下载文件", description = "根据文件 ID 输出文件内容流供浏览器下载。")
    @GetMapping("/{id}/content")
    public void download(@Parameter(description = "文件 ID") @PathVariable String id, HttpServletResponse response) {
        streamContent(id, response, false);
    }

    private void streamContent(String id, HttpServletResponse response, boolean inline) {
        try {
            FileMetadataDTO metadata = fileMetadataConvert.toDTO(fileDownloadService.resolve(id));
            try (var content = fileDownloadService.openContent(id)) {
                response.setContentType(metadata.getContentType());
                ContentDisposition.Builder disposition = ContentDisposition
                        .builder(inline ? "inline" : "attachment");
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                        disposition.filename(encode(metadata.getOriginalName())).build().toString());
                if (metadata.getFileSize() != null) {
                    response.setContentLengthLong(metadata.getFileSize());
                }
                content.transferTo(response.getOutputStream());
            }
        } catch (FileNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to stream file {}", id, e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    private String encode(String name) {
        if (name == null) {
            return "file";
        }
        return URLEncoder.encode(name, StandardCharsets.UTF_8);
    }
}
