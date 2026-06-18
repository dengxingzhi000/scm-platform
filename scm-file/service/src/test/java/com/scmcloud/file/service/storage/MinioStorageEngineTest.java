package com.scmcloud.file.service.storage;

import com.scmcloud.file.config.StorageConfig;
import com.scmcloud.file.entity.FileMetadata;
import io.minio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioStorageEngineTest {
    
    @Mock
    private MinioClient minioClient;
    
    @Mock
    private StorageConfig config;
    
    @InjectMocks
    private MinioStorageEngine engine;
    
    @BeforeEach
    void setUp() {
        when(config.getBucketName()).thenReturn("test-bucket");
    }
    
    @Test
    void shouldUploadFile() throws Exception {
        // Given
        byte[] fileBytes = "test content".getBytes();
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        
        // When
        FileMetadata result = engine.upload(fileBytes, "test.txt", "text/plain", 1L);
        
        // Then
        assertNotNull(result);
        assertEquals("test.txt", result.getOriginalName());
        assertEquals("text/plain", result.getContentType());
        assertEquals(12L, result.getFileSize());
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }
    
    @Test
    void shouldCheckFileExists() throws Exception {
        // Given
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
        
        // When
        boolean exists = engine.exists("test-key");
        
        // Then
        assertTrue(exists);
    }
}
