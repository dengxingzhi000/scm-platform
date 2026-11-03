package com.cognora.platform.file.service.management;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cognora.platform.file.entity.FileMetadata;
import com.cognora.platform.file.service.FileNotFoundException;
import com.cognora.platform.file.service.metadata.FileMetadataService;
import com.cognora.platform.file.service.storage.StorageEngine;
import com.cognora.platform.file.service.storage.StorageException;
import com.cognora.platform.file.service.storage.StorageFactory;
import com.cognora.platform.file.service.upload.FileValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileManagementServiceTest {

    private static final String TENANT = "tenant-1";

    @Mock
    private FileMetadataService metadataService;

    @Mock
    private StorageFactory storageFactory;

    @Mock
    private StorageEngine storageEngine;

    private FileManagementService service;

    @BeforeEach
    void setUp() {
        service = new FileManagementService(metadataService, storageFactory);
    }

    private FileMetadata metadata(String id, String storageKey) {
        FileMetadata metadata = new FileMetadata();
        metadata.setId(id);
        metadata.setTenantId(TENANT);
        metadata.setOriginalName("contract.pdf");
        metadata.setStorageKey(storageKey);
        return metadata;
    }

    @Test
    void update_shouldRenameAndRebindBusiness() {
        FileMetadata existing = metadata("file-1", "tenant-1/2026/contract.pdf");
        when(metadataService.findById("file-1", TENANT)).thenReturn(Optional.of(existing));

        FileMetadata updated = service.update("file-1", TENANT, " 新合同.pdf ", "CONTRACT", "order-1");

        assertEquals("新合同.pdf", updated.getOriginalName());
        assertEquals("CONTRACT", updated.getBizType());
        assertEquals("order-1", updated.getBizId());
        verify(metadataService).updateById(existing);
    }

    @Test
    void update_shouldClearBusinessAssociationWhenBlank() {
        FileMetadata existing = metadata("file-1", "tenant-1/2026/contract.pdf");
        existing.setBizType("CONTRACT");
        existing.setBizId("order-1");
        when(metadataService.findById("file-1", TENANT)).thenReturn(Optional.of(existing));

        FileMetadata updated = service.update("file-1", TENANT, null, "  ", "");

        assertNull(updated.getBizType());
        assertNull(updated.getBizId());
        verify(metadataService).updateById(existing);
    }

    @Test
    void update_shouldRejectBlankName() {
        FileMetadata existing = metadata("file-1", "tenant-1/2026/contract.pdf");
        when(metadataService.findById("file-1", TENANT)).thenReturn(Optional.of(existing));

        assertThrows(FileValidationException.class,
                () -> service.update("file-1", TENANT, "  ", null, null));
        verify(metadataService, never()).updateById(any());
    }

    @Test
    void update_shouldThrowWhenFileMissing() {
        when(metadataService.findById("missing", TENANT)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class,
                () -> service.update("missing", TENANT, "name", null, null));
    }

    @Test
    void delete_shouldRemoveStorageObjectAndMetadata() {
        FileMetadata existing = metadata("file-1", "tenant-1/2026/contract.pdf");
        when(metadataService.findById("file-1", TENANT)).thenReturn(Optional.of(existing));
        when(storageFactory.getDefaultEngine()).thenReturn(storageEngine);

        service.delete("file-1", TENANT);

        verify(storageEngine).delete("tenant-1/2026/contract.pdf");
        verify(metadataService).removeById("file-1");
    }

    @Test
    void delete_shouldRemoveMetadataEvenWhenStorageFails() {
        FileMetadata existing = metadata("file-1", "tenant-1/2026/contract.pdf");
        when(metadataService.findById("file-1", TENANT)).thenReturn(Optional.of(existing));
        when(storageFactory.getDefaultEngine()).thenReturn(storageEngine);
        doThrow(new StorageException("minio down"))
                .when(storageEngine).delete("tenant-1/2026/contract.pdf");

        service.delete("file-1", TENANT);

        verify(metadataService).removeById("file-1");
    }

    @Test
    void delete_shouldThrowWhenFileMissing() {
        when(metadataService.findById("missing", TENANT)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> service.delete("missing", TENANT));
        verify(storageFactory, never()).getDefaultEngine();
    }

    @Test
    void deleteBatch_shouldDeleteTenantFilesOnly() {
        FileMetadata first = metadata("file-1", "key-1");
        FileMetadata second = metadata("file-2", "key-2");
        when(metadataService.list(ArgumentMatchers.<Wrapper<FileMetadata>>any())).thenReturn(List.of(first, second));
        when(storageFactory.getDefaultEngine()).thenReturn(storageEngine);

        int deleted = service.deleteBatch(List.of("file-1", "file-1", "file-2", " "), TENANT);

        assertEquals(2, deleted);
        verify(storageEngine).delete("key-1");
        verify(storageEngine).delete("key-2");
        verify(metadataService).removeByIds(List.of("file-1", "file-2"));
    }

    @Test
    void deleteBatch_shouldShortCircuitOnEmptyInput() {
        assertEquals(0, service.deleteBatch(null, TENANT));
        assertEquals(0, service.deleteBatch(List.of(" "), TENANT));
        verify(metadataService, never()).list(ArgumentMatchers.<Wrapper<FileMetadata>>any());
        verify(metadataService, never()).removeByIds(any());
    }

    @Test
    void delete_shouldSkipStorageWhenKeyMissing() {
        FileMetadata existing = metadata("file-1", null);
        when(metadataService.findById("file-1", TENANT)).thenReturn(Optional.of(existing));

        service.delete("file-1", TENANT);

        verify(storageFactory, never()).getDefaultEngine();
        verify(metadataService).removeById("file-1");
    }
}