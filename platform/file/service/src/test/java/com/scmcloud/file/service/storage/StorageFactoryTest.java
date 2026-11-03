package com.scmcloud.file.service.storage;

import com.scmcloud.file.api.enums.StorageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageFactoryTest {
    
    @Test
    void shouldGetEngineByType() {
        // Given
        StorageEngine minioEngine = mock(StorageEngine.class);
        when(minioEngine.support()).thenReturn(StorageType.MINIO);
        
        StorageFactory factory = new StorageFactory(List.of(minioEngine));
        factory.init();
        
        // When
        StorageEngine result = factory.getEngine(StorageType.MINIO);
        
        // Then
        assertEquals(minioEngine, result);
    }
    
    @Test
    void shouldThrowExceptionForUnsupportedType() {
        // Given
        StorageFactory factory = new StorageFactory(List.of());
        factory.init();
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> factory.getEngine(StorageType.MINIO));
    }
}