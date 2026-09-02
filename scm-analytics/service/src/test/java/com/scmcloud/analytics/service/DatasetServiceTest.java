package com.scmcloud.analytics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.analytics.api.dto.DatasetDTO;
import com.scmcloud.analytics.domain.entity.DatasetDefinition;
import com.scmcloud.analytics.mapper.DatasetMapper;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.common.tenant.TenantContextHolder.TenantNotFoundException;
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
class DatasetServiceTest {

    @Mock
    private DatasetMapper datasetMapper;

    private DatasetService datasetService;

    private static final UUID TENANT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        datasetService = new DatasetService(datasetMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void testCreateWideTableDataset() {
        when(datasetMapper.insert(any(DatasetDefinition.class))).thenAnswer(inv -> {
            DatasetDefinition arg = inv.getArgument(0);
            arg.setId(UUID.randomUUID());
            return 1;
        });

        DatasetDTO dto = DatasetDTO.builder()
                .datasetCode("ORDERS_WIDE")
                .datasetName("Orders Wide Table")
                .description("Wide table view of orders")
                .datasetType("WIDE_TABLE")
                .sourceType("CLICKHOUSE")
                .sourceTable("dws_order")
                .sourceConfig("{\"database\":\"analytics\",\"table\":\"dws_order\"}")
                .enabled(true)
                .build();

        datasetService.create(dto);

        ArgumentCaptor<DatasetDefinition> captor = ArgumentCaptor.forClass(DatasetDefinition.class);
        verify(datasetMapper).insert(captor.capture());
        DatasetDefinition persisted = captor.getValue();
        assertEquals(TENANT_ID.toString(), persisted.getTenantId());
        assertEquals("ORDERS_WIDE", persisted.getDatasetCode());
        assertEquals("WIDE_TABLE", persisted.getDatasetType());
        assertEquals("CLICKHOUSE", persisted.getSourceType());
        assertEquals("dws_order", persisted.getSourceTable());
        assertEquals("{\"database\":\"analytics\",\"table\":\"dws_order\"}", persisted.getSourceConfig());
        assertTrue(persisted.getEnabled());
        assertEquals(Integer.valueOf(1), persisted.getStatus());
    }

    @Test
    void testCreateApiDataset() {
        when(datasetMapper.insert(any(DatasetDefinition.class))).thenAnswer(inv -> {
            DatasetDefinition arg = inv.getArgument(0);
            arg.setId(UUID.randomUUID());
            return 1;
        });

        DatasetDTO dto = DatasetDTO.builder()
                .datasetCode("EXCHANGE_RATES_API")
                .datasetName("Exchange Rates API")
                .datasetType("API")
                .sourceType("API")
                .sourceConfig("{\"url\":\"https://api.exchangerate.host/latest\"}")
                .build();

        datasetService.create(dto);

        ArgumentCaptor<DatasetDefinition> captor = ArgumentCaptor.forClass(DatasetDefinition.class);
        verify(datasetMapper).insert(captor.capture());
        DatasetDefinition persisted = captor.getValue();
        assertEquals("API", persisted.getDatasetType());
        assertEquals("API", persisted.getSourceType());
    }

    @Test
    void testCreateWithDuplicateCodeThrows() {
        when(datasetMapper.insert(any(DatasetDefinition.class)))
                .thenThrow(new DuplicateKeyException("uk_dataset_tenant_code"));

        DatasetDTO dto = DatasetDTO.builder()
                .datasetCode("DUPLICATE")
                .datasetName("Dup")
                .datasetType("WIDE_TABLE")
                .sourceType("CLICKHOUSE")
                .build();

        assertThrows(BusinessException.class, () -> datasetService.create(dto));
    }

    @Test
    void testUpdate() {
        UUID id = UUID.randomUUID();
        DatasetDefinition existing = DatasetDefinition.builder()
                .id(id)
                .tenantId(TENANT_ID.toString())
                .datasetCode("ORDERS_WIDE")
                .datasetName("Old")
                .datasetType("WIDE_TABLE")
                .sourceType("CLICKHOUSE")
                .enabled(true)
                .build();
        when(datasetMapper.selectById(id)).thenReturn(existing);
        when(datasetMapper.updateById(any(DatasetDefinition.class))).thenReturn(1);

        DatasetDTO dto = DatasetDTO.builder()
                .datasetCode("ORDERS_WIDE")
                .datasetName("Updated")
                .datasetType("WIDE_TABLE")
                .sourceType("CLICKHOUSE")
                .enabled(false)
                .build();

        DatasetDefinition result = datasetService.update(id, dto);

        assertEquals("Updated", result.getDatasetName());
        assertEquals(false, result.getEnabled());
        verify(datasetMapper).updateById(any(DatasetDefinition.class));
    }

    @Test
    void testUpdateNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(datasetMapper.selectById(id)).thenReturn(null);

        DatasetDTO dto = DatasetDTO.builder()
                .datasetCode("X").datasetName("X").datasetType("WIDE_TABLE").sourceType("CLICKHOUSE").build();

        assertThrows(BusinessException.class, () -> datasetService.update(id, dto));
        verify(datasetMapper, never()).updateById(any(DatasetDefinition.class));
    }

    @Test
    void testDelete() {
        UUID id = UUID.randomUUID();
        when(datasetMapper.deleteById(id)).thenReturn(1);

        datasetService.delete(id);

        verify(datasetMapper, times(1)).deleteById(id);
    }

    @Test
    void testDeleteNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(datasetMapper.deleteById(id)).thenReturn(0);

        assertThrows(BusinessException.class, () -> datasetService.delete(id));
    }

    @Test
    void testGetByCode() {
        DatasetDefinition entity = DatasetDefinition.builder()
                .tenantId(TENANT_ID.toString())
                .datasetCode("ORDERS_WIDE")
                .datasetType("WIDE_TABLE")
                .sourceType("CLICKHOUSE")
                .enabled(true)
                .build();
        when(datasetMapper.selectByTenantAndCode(eq(TENANT_ID.toString()), eq("ORDERS_WIDE")))
                .thenReturn(entity);

        DatasetDefinition result = datasetService.getByCode("ORDERS_WIDE");

        assertNotNull(result);
        verify(datasetMapper).selectByTenantAndCode(TENANT_ID.toString(), "ORDERS_WIDE");
    }

    @Test
    void testListByTenant() {
        DatasetDefinition d1 = DatasetDefinition.builder()
                .tenantId(TENANT_ID.toString())
                .datasetCode("D1").datasetType("WIDE_TABLE").sourceType("CLICKHOUSE").enabled(true).build();
        DatasetDefinition d2 = DatasetDefinition.builder()
                .tenantId(TENANT_ID.toString())
                .datasetCode("D2").datasetType("VIEW").sourceType("POSTGRES").enabled(true).build();

        Page<DatasetDefinition> mapperPage = new Page<>(1, 10);
        mapperPage.setRecords(List.of(d1, d2));
        mapperPage.setTotal(2);
        when(datasetMapper.selectPage(any(Page.class), any())).thenReturn(mapperPage);

        Page<DatasetDefinition> result = datasetService.list(1, 10, null);

        assertEquals(2, result.getTotal());
    }

    @Test
    void testCreateRequiresTenantContext() {
        TenantContextHolder.clear();
        DatasetDTO dto = DatasetDTO.builder()
                .datasetCode("D").datasetName("D").datasetType("WIDE_TABLE").sourceType("CLICKHOUSE").build();

        assertThrows(TenantNotFoundException.class, () -> datasetService.create(dto));
        verify(datasetMapper, never()).insert(any(DatasetDefinition.class));
    }

    @Test
    void testCreateRejectsInvalidDatasetType() {
        DatasetDTO dto = DatasetDTO.builder()
                .datasetCode("D").datasetName("D").datasetType("BOGUS").sourceType("CLICKHOUSE").build();

        assertThrows(BusinessException.class, () -> datasetService.create(dto));
        verify(datasetMapper, never()).insert(any(DatasetDefinition.class));
    }
}