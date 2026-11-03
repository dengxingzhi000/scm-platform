package com.cognora.platform.file.service.metadata;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.file.api.dto.FileMetadataDTO;
import com.cognora.platform.file.api.dto.FileStatsDTO;
import com.cognora.platform.file.convert.FileMetadataConvert;
import com.cognora.platform.file.entity.FileMetadata;
import com.cognora.platform.file.mapper.FileMetadataMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileMetadataServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("019a0aee-0000-7000-8000-000000000002");
    
    @Mock
    private FileMetadataMapper mapper;

    @Mock
    private FileMetadataConvert fileMetadataConvert;
    
    private FileMetadataService service;
    
    @BeforeEach
    void setUp() {
        service = new FileMetadataService(fileMetadataConvert);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
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
        when(mapper.selectOne(any(), any(Boolean.class))).thenReturn(metadata);
        
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

    @Test
    void pageFilesShouldKeepPagingMetadataAndMapRecordsToDto() {
        TenantContextHolder.setTenantId(TENANT_ID);
        FileMetadata metadata = new FileMetadata();
        metadata.setId("file-1");
        Page<FileMetadata> source = new Page<>(1, 20, 1);
        source.setRecords(List.of(metadata));
        FileMetadataDTO dto = new FileMetadataDTO();
        dto.setId("file-1");
        when(mapper.selectPage(any(), any())).thenReturn(source);
        when(fileMetadataConvert.toDTO(metadata)).thenReturn(dto);

        Page<FileMetadataDTO> result = service.pageFiles(1, 20, "contract", "CONTRACT", "order-1");

        assertEquals(1, result.getCurrent());
        assertEquals(20, result.getSize());
        assertEquals(1, result.getTotal());
        assertEquals("file-1", result.getRecords().get(0).getId());
        verify(fileMetadataConvert).toDTO(metadata);
    }

    @Test
    void getStatsShouldResolveTenantFromContext() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(mapper.selectStats(TENANT_ID.toString())).thenReturn(new FileStatsDTO());

        FileStatsDTO result = service.getStats();

        assertNotNull(result);
        verify(mapper).selectStats(TENANT_ID.toString());
    }
}
