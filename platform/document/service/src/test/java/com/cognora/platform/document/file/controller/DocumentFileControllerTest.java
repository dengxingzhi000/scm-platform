package com.cognora.platform.document.file.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.document.file.domain.entity.DocumentFile;
import com.cognora.platform.document.file.service.DocumentFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentFileControllerTest {

    @Mock
    private DocumentFileService documentFileService;

    @InjectMocks
    private DocumentFileController controller;

    @Test
    void pageShouldUseDefaultPagingAndDelegateToService() {
        Page<DocumentFile> page = new Page<>(1, 20, 0);
        when(documentFileService.pageCurrentTenant(1, 20, null, null, null)).thenReturn(page);

        ApiResponse<Page<DocumentFile>> response = controller.page(null, null, null, null, null);

        assertEquals(200, response.code());
        assertSame(page, response.data());
        verify(documentFileService).pageCurrentTenant(1, 20, null, null, null);
    }

    @Test
    void createShouldReturnTheServiceManagedEntity() {
        DocumentFile file = new DocumentFile();
        file.setOriginalName("contract.pdf");
        when(documentFileService.createCurrentTenant(file)).thenReturn(file);

        ApiResponse<DocumentFile> response = controller.create(file);

        assertEquals(200, response.code());
        assertSame(file, response.data());
        verify(documentFileService).createCurrentTenant(file);
    }

    @Test
    void deleteShouldDelegateOwnershipCheckToService() {
        when(documentFileService.deleteCurrentTenant("file-1")).thenReturn(false);

        ApiResponse<Void> response = controller.delete("file-1");

        assertEquals(200, response.code());
        verify(documentFileService).deleteCurrentTenant("file-1");
    }

    @Test
    void presignedUrlShouldReturn404WhenDocumentFileMissing() {
        when(documentFileService.findCurrentTenant("file-1")).thenReturn(Optional.empty());

        ApiResponse<java.util.Map<String, String>> response = controller.presignedUrl("file-1");

        assertEquals(404, response.code());
        assertEquals("document file not found", response.message());
    }

    @Test
    void presignedUrlShouldReturn404WhenStorageFileMissing() {
        DocumentFile file = new DocumentFile();
        file.setFileRef("storage-1");
        when(documentFileService.findCurrentTenant("file-1")).thenReturn(Optional.of(file));
        when(documentFileService.presignedUrlCurrentTenant("storage-1")).thenReturn(Optional.empty());

        ApiResponse<java.util.Map<String, String>> response = controller.presignedUrl("file-1");

        assertEquals(404, response.code());
        assertEquals("storage file not found", response.message());
    }

    @Test
    void getByIdShouldReturnNullPayloadWhenRecordMissing() {
        when(documentFileService.findCurrentTenant("file-1")).thenReturn(Optional.empty());

        ApiResponse<DocumentFile> response = controller.getById("file-1");

        assertEquals(200, response.code());
        assertEquals(null, response.data());
    }
}
