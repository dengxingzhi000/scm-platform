package com.scmcloud.file.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.file.api.dto.FileBatchDeleteRequest;
import com.scmcloud.file.api.dto.FileMetadataDTO;
import com.scmcloud.file.api.dto.FileStatsDTO;
import com.scmcloud.file.api.dto.FileUpdateRequest;
import com.scmcloud.file.convert.FileMetadataConvert;
import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.service.FileNotFoundException;
import com.scmcloud.file.service.download.FileDownloadService;
import com.scmcloud.file.service.management.FileManagementService;
import com.scmcloud.file.service.metadata.FileMetadataService;
import com.scmcloud.file.service.upload.FileValidationException;
import com.scmcloud.file.service.upload.InstantUploadService;
import com.scmcloud.file.service.upload.UploadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private UploadService uploadService;

    @Mock
    private InstantUploadService instantUploadService;

    @Mock
    private FileDownloadService fileDownloadService;

    @Mock
    private FileMetadataConvert fileMetadataConvert;

    @Mock
    private FileMetadataService fileMetadataService;

    @Mock
    private FileManagementService fileManagementService;

    @InjectMocks
    private FileController controller;

    @Test
    void pageShouldUseDefaultPagingAndDelegateToService() {
        Page<FileMetadataDTO> page = new Page<>(1, 20, 0);
        when(fileMetadataService.pageFiles(1, 20, "contract", "CONTRACT", "order-1")).thenReturn(page);

        ApiResponse<Page<FileMetadataDTO>> response =
                controller.page(null, null, "contract", "CONTRACT", "order-1");

        assertEquals(200, response.code());
        assertEquals(page, response.data());
    }

    @Test
    void statsShouldDelegateToService() {
        FileStatsDTO stats = new FileStatsDTO();
        when(fileMetadataService.getStats()).thenReturn(stats);

        ApiResponse<FileStatsDTO> response = controller.stats();

        assertEquals(200, response.code());
        assertEquals(stats, response.data());
    }

    @Test
    void updateShouldKeep404ForMissingFile() {
        FileUpdateRequest request = new FileUpdateRequest();
        when(fileManagementService.update("file-1", "contract.pdf", "CONTRACT", "order-1"))
                .thenThrow(new FileNotFoundException("file-1"));

        ApiResponse<FileMetadataDTO> response = controller.update("file-1", requestWith("contract.pdf", "CONTRACT", "order-1"));

        assertEquals(404, response.code());
        assertEquals("File not found: file-1", response.message());
    }

    @Test
    void updateShouldHideUnexpectedExceptionDetail() {
        FileUpdateRequest request = requestWith("contract.pdf", null, null);
        when(fileManagementService.update("file-1", "contract.pdf", null, null))
                .thenThrow(new RuntimeException("db jdbc:postgresql://admin:secret@host"));

        ApiResponse<FileMetadataDTO> response = controller.update("file-1", request);

        assertEquals(500, response.code());
        assertEquals("Update failed", response.message());
    }

    @Test
    void deleteShouldReturn404WhenMissing() {
        doThrow(new FileNotFoundException("file-1")).when(fileManagementService).delete("file-1");

        ApiResponse<Void> response = controller.delete("file-1");

        assertEquals(404, response.code());
    }

    @Test
    void batchDeleteShouldDelegateToCurrentTenantService() {
        FileBatchDeleteRequest request = new FileBatchDeleteRequest();
        request.setIds(List.of("file-1", "file-2"));
        when(fileManagementService.deleteBatch(request.getIds())).thenReturn(2);

        ApiResponse<Integer> response = controller.batchDelete(request);

        assertEquals(200, response.code());
        assertEquals(2, response.data());
    }

    @Test
    void uploadShouldKeepValidationMessageWhileHidingInternalFailure() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "data".getBytes());
        when(uploadService.upload(any(MultipartFile.class), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("No space left on device: /data/minio/secret"));

        ApiResponse<FileMetadataDTO> response = controller.upload(file, null, null);

        assertEquals(500, response.code());
        assertEquals("Upload failed", response.message());
    }

    @Test
    void uploadShouldReturn400WithValidationMessage() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "data".getBytes());
        when(uploadService.upload(any(MultipartFile.class), any(), any(), any(), any()))
                .thenThrow(new FileValidationException("File type not allowed"));

        ApiResponse<FileMetadataDTO> response = controller.upload(file, null, null);

        assertEquals(400, response.code());
        assertEquals("File type not allowed", response.message());
    }

    @Test
    void uploadSuccessShouldReturnMappedMetadata() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "data".getBytes());
        FileMetadata metadata = new FileMetadata();
        metadata.setId("file-1");
        FileMetadataDTO dto = new FileMetadataDTO();
        dto.setId("file-1");
        when(uploadService.upload(any(MultipartFile.class), any(), any(), any(), any())).thenReturn(metadata);
        when(fileMetadataConvert.toDTO(metadata)).thenReturn(dto);

        ApiResponse<FileMetadataDTO> response = controller.upload(file, null, null);

        assertEquals(200, response.code());
        assertEquals("file-1", response.data().getId());
    }

    @Test
    void checkExistShouldHideExceptionDetail() {
        when(instantUploadService.checkExist("md5-1")).thenThrow(new RuntimeException("redis password=secret"));

        ApiResponse<FileMetadataDTO> response = controller.checkExist("md5-1");

        assertEquals(500, response.code());
        assertEquals("Check failed", response.message());
    }

    @Test
    void checkExistShouldReturnEmptyPayloadWhenUnknown() {
        when(instantUploadService.checkExist("md5-1")).thenReturn(Optional.empty());

        ApiResponse<FileMetadataDTO> response = controller.checkExist("md5-1");

        assertEquals(200, response.code());
        assertEquals(null, response.data());
    }

    @Test
    void presignedUrlShouldKeep404ForMissingFile() {
        when(fileDownloadService.generatePresignedUrl("file-1")).thenThrow(new FileNotFoundException("file-1"));

        ApiResponse<Map<String, String>> response = controller.presignedUrl("file-1");

        assertEquals(404, response.code());
    }

    @Test
    void presignedUrlShouldHideExceptionDetail() {
        when(fileDownloadService.generatePresignedUrl("file-1"))
                .thenThrow(new RuntimeException("minio secret key leaked"));

        ApiResponse<Map<String, String>> response = controller.presignedUrl("file-1");

        assertEquals(500, response.code());
        assertEquals("Failed to generate url", response.message());
    }

    @Test
    void downloadShouldReturn404StatusForMissingFile() {
        when(fileDownloadService.resolve("file-1")).thenThrow(new FileNotFoundException("file-1"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.download("file-1", response);

        assertEquals(404, response.getStatus());
        // getContentAsString() throws a checked UnsupportedEncodingException,
        // so read the bytes instead.
        assertEquals(0, response.getContentAsByteArray().length);
    }

    private FileUpdateRequest requestWith(String name, String bizType, String bizId) {
        FileUpdateRequest request = new FileUpdateRequest();
        request.setOriginalName(name);
        request.setBizType(bizType);
        request.setBizId(bizId);
        return request;
    }
}
