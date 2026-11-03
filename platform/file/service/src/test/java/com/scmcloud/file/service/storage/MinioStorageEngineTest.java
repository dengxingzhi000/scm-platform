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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
        StorageConfig.Minio minio = new StorageConfig.Minio();
        minio.setBucketName("test-bucket");
        minio.setCreateBucket(false);
        when(config.getMinio()).thenReturn(minio);
    }

    @Test
    void shouldUploadFile() throws Exception {
        // Given
        byte[] fileBytes = "test content".getBytes();
        InputStream content = new ByteArrayInputStream(fileBytes);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        // When
        FileMetadata result = engine.upload(content, fileBytes.length, "test.txt", "text/plain", "tenant-1");

        // Then
        assertNotNull(result);
        assertEquals("test.txt", result.getOriginalName());
        assertEquals("text/plain", result.getContentType());
        assertEquals(fileBytes.length, result.getFileSize());
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
