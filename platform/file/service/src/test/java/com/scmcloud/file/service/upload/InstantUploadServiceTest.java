package com.scmcloud.file.service.upload;

import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.service.metadata.FileMetadataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstantUploadServiceTest {
    
    @Mock
    private FileMetadataService metadataService;
    
    @InjectMocks
    private InstantUploadService service;
    
    @Test
    void shouldReturnExistingFileWhenMd5Matches() {
        // Given
        String md5 = "abc123";
        String tenantId = "tenant-1";
        FileMetadata existing = new FileMetadata();
        existing.setId("existing-id");
        existing.setMd5(md5);
        when(metadataService.findByMd5(md5, tenantId)).thenReturn(Optional.of(existing));
        
        // When
        Optional<FileMetadata> result = service.checkExist(md5, tenantId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals("existing-id", result.get().getId());
    }
    
    @Test
    void shouldReturnEmptyWhenMd5NotExists() {
        // Given
        String md5 = "not-exists";
        String tenantId = "tenant-1";
        when(metadataService.findByMd5(md5, tenantId)).thenReturn(Optional.empty());
        
        // When
        Optional<FileMetadata> result = service.checkExist(md5, tenantId);
        
        // Then
        assertFalse(result.isPresent());
    }
}
