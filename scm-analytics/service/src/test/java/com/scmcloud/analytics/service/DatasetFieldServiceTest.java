package com.scmcloud.analytics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.analytics.api.dto.DatasetFieldDTO;
import com.scmcloud.analytics.domain.entity.DatasetFieldDefinition;
import com.scmcloud.analytics.mapper.DatasetFieldMapper;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.common.tenant.TenantParseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetFieldServiceTest {

    @Mock
    private DatasetFieldMapper datasetFieldMapper;

    private DatasetFieldService datasetFieldService;

    private static final UUID TENANT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID DATASET_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        datasetFieldService = new DatasetFieldService(datasetFieldMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void testCreateMetricField() {
        when(datasetFieldMapper.insert(any(DatasetFieldDefinition.class))).thenAnswer(inv -> {
            DatasetFieldDefinition arg = inv.getArgument(0);
            arg.setId(UUID.randomUUID());
            return 1;
        });

        DatasetFieldDTO dto = DatasetFieldDTO.builder()
                .datasetId(DATASET_ID)
                .fieldCode("ORDER_AMOUNT")
                .fieldName("Order Amount")
                .fieldType("METRIC")
                .dataType("DECIMAL")
                .metricId(UUID.randomUUID())
                .isRequired(false)
                .sortOrder(1)
                .build();

        datasetFieldService.create(dto);

        ArgumentCaptor<DatasetFieldDefinition> captor = ArgumentCaptor.forClass(DatasetFieldDefinition.class);
        verify(datasetFieldMapper).insert(captor.capture());
        DatasetFieldDefinition persisted = captor.getValue();
        assertEquals(TENANT_ID.toString(), persisted.getTenantId());
        assertEquals(DATASET_ID, persisted.getDatasetId());
        assertEquals("ORDER_AMOUNT", persisted.getFieldCode());
        assertEquals("METRIC", persisted.getFieldType());
        assertEquals("DECIMAL", persisted.getDataType());
        assertNotNull(persisted.getMetricId());
        assertEquals(Integer.valueOf(1), persisted.getSortOrder());
        assertEquals(Integer.valueOf(1), persisted.getStatus());
        assertTrue(persisted.getIsHidden() == null || !persisted.getIsHidden());
        assertTrue(persisted.getIsPartitionKey() == null || !persisted.getIsPartitionKey());
    }

    @Test
    void testCreateDimensionField() {
        when(datasetFieldMapper.insert(any(DatasetFieldDefinition.class))).thenAnswer(inv -> {
            DatasetFieldDefinition arg = inv.getArgument(0);
            arg.setId(UUID.randomUUID());
            return 1;
        });

        DatasetFieldDTO dto = DatasetFieldDTO.builder()
                .datasetId(DATASET_ID)
                .fieldCode("ORDER_DATE")
                .fieldName("Order Date")
                .fieldType("DIMENSION")
                .dataType("DATE")
                .dimensionId(UUID.randomUUID())
                .isPartitionKey(true)
                .build();

        datasetFieldService.create(dto);

        ArgumentCaptor<DatasetFieldDefinition> captor = ArgumentCaptor.forClass(DatasetFieldDefinition.class);
        verify(datasetFieldMapper).insert(captor.capture());
        DatasetFieldDefinition persisted = captor.getValue();
        assertEquals("DIMENSION", persisted.getFieldType());
        assertNotNull(persisted.getDimensionId());
        assertTrue(persisted.getIsPartitionKey());
    }

    @Test
    void testCreateCalculatedField() {
        when(datasetFieldMapper.insert(any(DatasetFieldDefinition.class))).thenAnswer(inv -> {
            DatasetFieldDefinition arg = inv.getArgument(0);
            arg.setId(UUID.randomUUID());
            return 1;
        });

        DatasetFieldDTO dto = DatasetFieldDTO.builder()
                .datasetId(DATASET_ID)
                .fieldCode("GROSS_MARGIN")
                .fieldName("Gross Margin")
                .fieldType("CALCULATED")
                .dataType("DECIMAL")
                .expr("(REVENUE - COST) / REVENUE")
                .build();

        datasetFieldService.create(dto);

        ArgumentCaptor<DatasetFieldDefinition> captor = ArgumentCaptor.forClass(DatasetFieldDefinition.class);
        verify(datasetFieldMapper).insert(captor.capture());
        DatasetFieldDefinition persisted = captor.getValue();
        assertEquals("CALCULATED", persisted.getFieldType());
        assertEquals("(REVENUE - COST) / REVENUE", persisted.getExpr());
    }

    @Test
    void testCreateWithDuplicateFieldCodeThrows() {
        when(datasetFieldMapper.insert(any(DatasetFieldDefinition.class)))
                .thenThrow(new DuplicateKeyException("uk_dataset_field_code"));

        DatasetFieldDTO dto = DatasetFieldDTO.builder()
                .datasetId(DATASET_ID)
                .fieldCode("DUPLICATE")
                .fieldName("Dup")
                .fieldType("CALCULATED")
                .dataType("DECIMAL")
                .expr("REVENUE - COST")
                .build();

        assertThrows(BusinessException.class, () -> datasetFieldService.create(dto));
    }

    @Test
    void testUpdate() {
        UUID id = UUID.randomUUID();
        DatasetFieldDefinition existing = DatasetFieldDefinition.builder()
                .id(id)
                .tenantId(TENANT_ID.toString())
                .datasetId(DATASET_ID)
                .fieldCode("ORDER_AMOUNT")
                .fieldName("Old")
                .fieldType("METRIC")
                .metricId(UUID.randomUUID())
                .dataType("DECIMAL")
                .build();
        when(datasetFieldMapper.selectById(id)).thenReturn(existing);
        when(datasetFieldMapper.updateById(any(DatasetFieldDefinition.class))).thenReturn(1);

        DatasetFieldDTO dto = DatasetFieldDTO.builder()
                .datasetId(DATASET_ID)
                .fieldCode("ORDER_AMOUNT")
                .fieldName("Updated")
                .fieldType("METRIC")
                .metricId(UUID.randomUUID())
                .dataType("DECIMAL")
                .build();

        DatasetFieldDefinition result = datasetFieldService.update(id, dto);

        assertEquals("Updated", result.getFieldName());
        verify(datasetFieldMapper).updateById(any(DatasetFieldDefinition.class));
    }

    @Test
    void testUpdateNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(datasetFieldMapper.selectById(id)).thenReturn(null);

        DatasetFieldDTO dto = DatasetFieldDTO.builder()
                .datasetId(DATASET_ID).fieldCode("X").fieldName("X").fieldType("CALCULATED").dataType("VARCHAR").build();

        assertThrows(BusinessException.class, () -> datasetFieldService.update(id, dto));
        verify(datasetFieldMapper, never()).updateById(any(DatasetFieldDefinition.class));
    }

    @Test
    void testDelete() {
        UUID id = UUID.randomUUID();
        when(datasetFieldMapper.deleteById(id)).thenReturn(1);

        datasetFieldService.delete(id);

        verify(datasetFieldMapper, times(1)).deleteById(id);
    }

    @Test
    void testDeleteNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(datasetFieldMapper.deleteById(id)).thenReturn(0);

        assertThrows(BusinessException.class, () -> datasetFieldService.delete(id));
    }

    @Test
    void testListByDataset() {
        DatasetFieldDefinition f1 = DatasetFieldDefinition.builder()
                .tenantId(TENANT_ID.toString())
                .datasetId(DATASET_ID)
                .fieldCode("F1")
                .fieldType("DIMENSION")
                .dataType("VARCHAR")
                .sortOrder(1)
                .build();
        DatasetFieldDefinition f2 = DatasetFieldDefinition.builder()
                .tenantId(TENANT_ID.toString())
                .datasetId(DATASET_ID)
                .fieldCode("F2")
                .fieldType("METRIC")
                .dataType("DECIMAL")
                .sortOrder(2)
                .build();

        when(datasetFieldMapper.selectByDataset(eq(TENANT_ID.toString()), eq(DATASET_ID)))
                .thenReturn(List.of(f1, f2));

        List<DatasetFieldDefinition> result = datasetFieldService.listByDataset(DATASET_ID);

        assertEquals(2, result.size());
        assertEquals("F1", result.get(0).getFieldCode());
        assertEquals("F2", result.get(1).getFieldCode());
        verify(datasetFieldMapper).selectByDataset(TENANT_ID.toString(), DATASET_ID);
    }

    @Test
    void testCreateRequiresTenantContext() {
        TenantContextHolder.clear();
        DatasetFieldDTO dto = DatasetFieldDTO.builder()
                .datasetId(DATASET_ID).fieldCode("F").fieldName("F").fieldType("CALCULATED").dataType("VARCHAR").build();

        assertThrows(TenantParseException.class, () -> datasetFieldService.create(dto));
        verify(datasetFieldMapper, never()).insert(any(DatasetFieldDefinition.class));
    }

    @Test
    void testCreateRequiresDatasetId() {
        DatasetFieldDTO dto = DatasetFieldDTO.builder()
                .fieldCode("F").fieldName("F").fieldType("DIMENSION").dataType("VARCHAR").build();

        assertThrows(BusinessException.class, () -> datasetFieldService.create(dto));
        verify(datasetFieldMapper, never()).insert(any(DatasetFieldDefinition.class));
    }

    @Test
    void testCreateRejectsInvalidFieldType() {
        DatasetFieldDTO dto = DatasetFieldDTO.builder()
                .datasetId(DATASET_ID).fieldCode("F").fieldName("F").fieldType("BOGUS").dataType("VARCHAR").build();

        assertThrows(BusinessException.class, () -> datasetFieldService.create(dto));
        verify(datasetFieldMapper, never()).insert(any(DatasetFieldDefinition.class));
    }
}