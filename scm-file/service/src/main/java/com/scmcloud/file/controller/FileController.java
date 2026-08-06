package com.scmcloud.file.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.file.api.dto.FileMetadataDTO;
import com.scmcloud.file.convert.FileMetadataConvert;
import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.service.upload.InstantUploadService;
import com.scmcloud.file.service.upload.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final UploadService uploadService;
    private final InstantUploadService instantUploadService;
    private final FileMetadataConvert fileMetadataConvert;

    @PostMapping("/upload")
    public ApiResponse<FileMetadataDTO> upload(@RequestParam("file") MultipartFile file,
                                                @RequestParam(value = "bizType", required = false) String bizType,
                                                @RequestParam(value = "bizId", required = false) String bizId) {
        try {
            Long tenantId = TenantContextHolder.getTenantId() != null
                    ? TenantContextHolder.getTenantId().getMostSignificantBits() : 1L;
            FileMetadata metadata = uploadService.upload(
                    file.getBytes(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    bizType,
                    bizId,
                    tenantId
            );
            return ApiResponse.success(fileMetadataConvert.toDTO(metadata));
        } catch (Exception e) {
            return ApiResponse.fail(500, "Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/check/{md5}")
    public ApiResponse<FileMetadataDTO> checkExist(@PathVariable String md5) {
        Long tenantId = TenantContextHolder.getTenantId() != null
                ? TenantContextHolder.getTenantId().getMostSignificantBits() : 1L;
        return instantUploadService.checkExist(md5, tenantId)
                .map(fileMetadataConvert::toDTO)
                .map(ApiResponse::success)
                .orElse(ApiResponse.success(null));
    }
}
