package com.scmcloud.analytics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.analytics.api.dto.DimensionDTO;
import com.scmcloud.analytics.domain.entity.DimensionDefinition;
import com.scmcloud.analytics.mapper.DimensionMapper;
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
class DimensionServiceTest {

    @Mock
    private DimensionMapper dimensionMapper;

    private DimensionService dimensionService;

    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        dimensionService = new DimensionService(dimensionMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void testCreateCategoricalDimension() {
        when(dimensionMapper.insert(any(DimensionDefinition.class))).thenAnswer(inv -> {
            DimensionDefinition arg = inv.getArgument(0);
            arg.setId(UUID.randomUUID());
            return 1;
        });

        DimensionDTO dto = DimensionDTO.builder()
                .dimCode("CATEGORY")
                .dimName("Product Category")
                .description("Top-level product category")
                .dimType("CATEGORICAL")
                .dataType("VARCHAR")
                .isRequired(true)
                .dictCode("PRODUCT_CATEGORY")
                .build();

        dimensionService.create(dto);

        ArgumentCaptor<DimensionDefinition> captor = ArgumentCaptor.forClass(DimensionDefinition.class);
        verify(dimensionMapper).insert(captor.capture());
        DimensionDefinition persisted = captor.getValue();
        assertEquals(TENANT_ID.toString(), persisted.getTenantId());
        assertEquals("CATEGORY", persisted.getDimCode());
        assertEquals("CATEGORICAL", persisted.getDimType());
        assertEquals("VARCHAR", persisted.getDataType());
        assertTrue(persisted.getIsRequired());
        assertEquals("PRODUCT_CATEGORY", persisted.getDictCode());
        assertEquals(Integer.valueOf(1), persisted.getHierarchyLevel());
        assertEquals(Integer.valueOf(1), persisted.getStatus());
    }

    @Test
    void testCreateTimeDimension() {
        when(dimensionMapper.insert(any(DimensionDefinition.class))).thenAnswer(inv -> {
            DimensionDefinition arg = inv.getArgument(0);
            arg.setId(UUID.randomUUID());
            return 1;
        });

        DimensionDTO dto = DimensionDTO.builder()
                .dimCode("ORDER_DATE")
                .dimName("Order Date")
                .dimType("TIME")
                .dataType("DATE")
                .hierarchyLevel(1)
                .build();

        dimensionService.create(dto);

        ArgumentCaptor<DimensionDefinition> captor = ArgumentCaptor.forClass(DimensionDefinition.class);
        verify(dimensionMapper).insert(captor.capture());
        DimensionDefinition persisted = captor.getValue();
        assertEquals("ORDER_DATE", persisted.getDimCode());
        assertEquals("TIME", persisted.getDimType());
        assertEquals("DATE", persisted.getDataType());
    }

    @Test
    void testCreateHierarchicalDimension() {
        UUID parentId = UUID.randomUUID();
        when(dimensionMapper.insert(any(DimensionDefinition.class))).thenAnswer(inv -> {
            DimensionDefinition arg = inv.getArgument(0);
            arg.setId(UUID.randomUUID());
            return 1;
        });

        DimensionDTO dto = DimensionDTO.builder()
                .dimCode("REGION")
                .dimName("Region")
                .dimType("HIERARCHICAL")
                .dataType("VARCHAR")
                .parentDimId(parentId)
                .hierarchyLevel(2)
                .build();

        dimensionService.create(dto);

        ArgumentCaptor<DimensionDefinition> captor = ArgumentCaptor.forClass(DimensionDefinition.class);
        verify(dimensionMapper).insert(captor.capture());
        DimensionDefinition persisted = captor.getValue();
        assertEquals(parentId, persisted.getParentDimId());
        assertEquals(Integer.valueOf(2), persisted.getHierarchyLevel());
    }

    @Test
    void testCreateWithDuplicateCodeThrows() {
        when(dimensionMapper.insert(any(DimensionDefinition.class)))
                .thenThrow(new DuplicateKeyException("uk_dimension_tenant_code"));

        DimensionDTO dto = DimensionDTO.builder()
                .dimCode("DUPLICATE")
                .dimName("Dup")
                .dimType("CATEGORICAL")
                .dataType("VARCHAR")
                .build();

        assertThrows(BusinessException.class, () -> dimensionService.create(dto));
    }

    @Test
    void testUpdate() {
        UUID id = UUID.randomUUID();
        DimensionDefinition existing = DimensionDefinition.builder()
                .id(id)
                .tenantId(TENANT_ID.toString())
                .dimCode("CATEGORY")
                .dimName("Old")
                .dimType("CATEGORICAL")
                .dataType("VARCHAR")
                .build();
        when(dimensionMapper.selectById(id)).thenReturn(existing);
        when(dimensionMapper.updateById(any(DimensionDefinition.class))).thenReturn(1);

        DimensionDTO dto = DimensionDTO.builder()
                .dimCode("CATEGORY")
                .dimName("Updated")
                .dimType("CATEGORICAL")
                .dataType("VARCHAR")
                .build();

        DimensionDefinition result = dimensionService.update(id, dto);

        assertEquals("Updated", result.getDimName());
        verify(dimensionMapper).updateById(any(DimensionDefinition.class));
    }

    @Test
    void testUpdateNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(dimensionMapper.selectById(id)).thenReturn(null);

        DimensionDTO dto = DimensionDTO.builder()
                .dimCode("X").dimName("X").dimType("CATEGORICAL").dataType("VARCHAR").build();

        assertThrows(BusinessException.class, () -> dimensionService.update(id, dto));
        verify(dimensionMapper, never()).updateById(any(DimensionDefinition.class));
    }

    @Test
    void testDelete() {
        UUID id = UUID.randomUUID();
        when(dimensionMapper.deleteById(id)).thenReturn(1);

        dimensionService.delete(id);

        verify(dimensionMapper).deleteById(id);
    }

    @Test
    void testDeleteNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(dimensionMapper.deleteById(id)).thenReturn(0);

        assertThrows(BusinessException.class, () -> dimensionService.delete(id));
    }

    @Test
    void testGetByCode() {
        DimensionDefinition entity = DimensionDefinition.builder()
                .tenantId(TENANT_ID.toString())
                .dimCode("CATEGORY")
                .dimType("CATEGORICAL")
                .dataType("VARCHAR")
                .build();
        when(dimensionMapper.selectByTenantAndCode(eq(TENANT_ID.toString()), eq("CATEGORY")))
                .thenReturn(entity);

        DimensionDefinition result = dimensionService.getByCode("CATEGORY");

        assertNotNull(result);
        verify(dimensionMapper).selectByTenantAndCode(TENANT_ID.toString(), "CATEGORY");
    }

    @Test
    void testListByTenant() {
        DimensionDefinition d1 = DimensionDefinition.builder()
                .tenantId(TENANT_ID.toString())
                .dimCode("D1")
                .dimType("TIME")
                .dataType("DATE")
                .build();
        DimensionDefinition d2 = DimensionDefinition.builder()
                .tenantId(TENANT_ID.toString())
                .dimCode("D2")
                .dimType("CATEGORICAL")
                .dataType("VARCHAR")
                .build();

        Page<DimensionDefinition> mapperPage = new Page<>(1, 10);
        mapperPage.setRecords(List.of(d1, d2));
        mapperPage.setTotal(2);
        when(dimensionMapper.selectPage(any(Page.class), any())).thenReturn(mapperPage);

        Page<DimensionDefinition> result = dimensionService.list(1, 10, null);

        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());
    }

    @Test
    void testCreateRequiresTenantContext() {
        TenantContextHolder.clear();
        DimensionDTO dto = DimensionDTO.builder()
                .dimCode("D").dimName("D").dimType("CATEGORICAL").dataType("VARCHAR").build();

        assertThrows(TenantNotFoundException.class, () -> dimensionService.create(dto));
        verify(dimensionMapper, never()).insert(any(DimensionDefinition.class));
    }

    @Test
    void testCreateRejectsInvalidHierarchyLevel() {
        DimensionDTO dto = DimensionDTO.builder()
                .dimCode("D").dimName("D").dimType("HIERARCHICAL").dataType("VARCHAR")
                .hierarchyLevel(10).build();

        assertThrows(BusinessException.class, () -> dimensionService.create(dto));
        verify(dimensionMapper, never()).insert(any(DimensionDefinition.class));
    }

    @Test
    void testCreateRejectsInvalidDimType() {
        DimensionDTO dto = DimensionDTO.builder()
                .dimCode("D").dimName("D").dimType("BOGUS").dataType("VARCHAR").build();

        assertThrows(BusinessException.class, () -> dimensionService.create(dto));
        verify(dimensionMapper, never()).insert(any(DimensionDefinition.class));
    }
}