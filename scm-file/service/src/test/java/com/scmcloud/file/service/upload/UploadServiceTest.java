package com.scmcloud.file.service.upload;

import com.scmcloud.file.config.StorageConfig;
import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.mapper.UploadTaskMapper;
import com.scmcloud.file.service.metadata.FileMetadataService;
import com.scmcloud.file.service.scan.FileVirusScanner;
import com.scmcloud.file.service.storage.StorageEngine;
import com.scmcloud.file.service.storage.StorageException;
import com.scmcloud.file.service.storage.StorageFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @Mock
    private StorageFactory storageFactory;
    @Mock
    private FileMetadataService metadataService;
    @Mock
    private InstantUploadService instantUploadService;
    @Mock
    private UploadTaskMapper uploadTaskMapper;
    @Mock
    private FileUploadValidator fileUploadValidator;
    @Mock
    private FileVirusScanner virusScanner;
    @Mock
    private StorageConfig storageConfig;
    @Mock
    private StorageEngine storageEngine;

    @InjectMocks
    private UploadService uploadService;

    @Test
    void shouldReturnExistingMetadataOnDedup() {
        FileMetadata existing = new FileMetadata();
        existing.setId("existing");
        when(instantUploadService.checkExist(anyString(), anyString())).thenReturn(Optional.of(existing));

        MultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "data".getBytes());
        FileMetadata result = uploadService.upload(file, "a.txt", "text/plain", null, null, "tenant-1");

        assertEquals("existing", result.getId());
        verify(storageEngine, never()).upload(any(), anyLong(), any(), any(), any());
    }

    @Test
    void shouldThrowWhenVirusScanFails() {
        when(instantUploadService.checkExist(anyString(), anyString())).thenReturn(Optional.empty());
        when(virusScanner.enabled()).thenReturn(true);
        when(virusScanner.scan(any(InputStream.class))).thenReturn(FileVirusScanner.ScanResult.infected("eicar"));

        MultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "data".getBytes());
        FileValidationException ex = assertThrows(FileValidationException.class,
                () -> uploadService.upload(file, "a.txt", "text/plain", null, null, "tenant-1"));
        assertTrue(ex.getMessage().contains("virus"));
    }

    @Test
    void shouldRollbackStorageObjectWhenMetadataSaveFails() {
        when(instantUploadService.checkExist(anyString(), anyString())).thenReturn(Optional.empty());
        when(virusScanner.enabled()).thenReturn(false);
        when(storageFactory.getDefaultEngine()).thenReturn(storageEngine);
        FileMetadata stored = new FileMetadata();
        stored.setStorageKey("key/abc.txt");
        when(storageEngine.upload(any(InputStream.class), anyLong(), any(), any(), any())).thenReturn(stored);
        doThrow(new RuntimeException("db down")).when(metadataService).saveMetadata(any(FileMetadata.class));

        MultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "data".getBytes());
        assertThrows(StorageException.class,
                () -> uploadService.upload(file, "a.txt", "text/plain", null, null, "tenant-1"));
        verify(storageEngine).delete("key/abc.txt");
    }

    @Test
    void shouldUploadSuccessfully() {
        when(instantUploadService.checkExist(anyString(), anyString())).thenReturn(Optional.empty());
        when(virusScanner.enabled()).thenReturn(false);
        when(storageFactory.getDefaultEngine()).thenReturn(storageEngine);
        FileMetadata stored = new FileMetadata();
        stored.setStorageKey("key/abc.txt");
        when(storageEngine.upload(any(InputStream.class), anyLong(), any(), any(), any())).thenReturn(stored);

        MultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "data".getBytes());
        FileMetadata result = uploadService.upload(file, "a.txt", "text/plain", null, null, "tenant-1");

        assertNotNull(result);
        verify(metadataService).saveMetadata(any(FileMetadata.class));
        verify(storageEngine, never()).delete(anyString());
    }
}
