package com.scmcloud.file.service.metadata;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scmcloud.file.api.dto.FileStatsDTO;
import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.mapper.FileMetadataMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileMetadataServiceTest {
    
    @Mock
    private FileMetadataMapper mapper;
    
    private FileMetadataService service;
    
    @BeforeEach
    void setUp() {
        service = new FileMetadataService();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }
    
    @Test
    void shouldSaveFileMetadata() {
        // Given
        FileMetadata metadata = new FileMetadata();
        metadata.setId("test-id");
        metadata.setOriginalName("test.txt");
        when(mapper.insert(any(FileMetadata.class))).thenReturn(1);
        
        // When
        FileMetadata result = service.saveMetadata(metadata);
        
        // Then
        assertNotNull(result);
        verify(mapper).insert(metadata);
    }
    
    @Test
    void shouldFindByMd5() {
        // Given
        String md5 = "abc123";
        FileMetadata metadata = new FileMetadata();
        metadata.setMd5(md5);
        when(mapper.selectOne(any(LambdaQueryWrapper.class), any(Boolean.class))).thenReturn(metadata);
        
        // When
        Optional<FileMetadata> result = service.findByMd5(md5, "tenant-1");
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(md5, result.get().getMd5());
    }

    @Test
    void shouldComputeStatsAndOtherBucket() {
        FileStatsDTO stats = new FileStatsDTO();
        stats.setTotalFiles(10L);
        stats.setTotalSize(1024L);
        stats.setImageFiles(3L);
        stats.setDocumentFiles(4L);
        when(mapper.selectStats("tenant-1")).thenReturn(stats);

        FileStatsDTO result = service.getStats("tenant-1");

        assertEquals(10L, result.getTotalFiles());
        assertEquals(3L, result.getOtherFiles());
        verify(mapper).selectStats("tenant-1");
    }

    @Test
    void shouldReturnZeroStatsWhenQueryReturnsNothing() {
        when(mapper.selectStats("tenant-1")).thenReturn(null);

        FileStatsDTO result = service.getStats("tenant-1");

        assertEquals(0L, result.getTotalFiles());
        assertEquals(0L, result.getTotalSize());
        assertEquals(0L, result.getOtherFiles());
    }
}
