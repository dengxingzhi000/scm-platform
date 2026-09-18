package com.scmcloud.analytics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.analytics.api.dto.MetricDTO;
import com.scmcloud.analytics.domain.entity.MetricDefinition;
import com.scmcloud.analytics.mapper.MetricMapper;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricServiceTest {

    @Mock
    private MetricMapper metricMapper;

    private MetricService metricService;

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        metricService = new MetricService(metricMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void testCreateAtomicMetric() {
        MetricDefinition captured = new MetricDefinition();
        when(metricMapper.insert(any(MetricDefinition.class))).thenAnswer(inv -> {
            MetricDefinition arg = inv.getArgument(0);
            arg.setId(UUID.randomUUID());
            captured.setMetricCode(arg.getMetricCode());
            captured.setMetricType(arg.getMetricType());
            captured.setAggFunc(arg.getAggFunc());
            captured.setExpr(arg.getExpr());
            captured.setTenantId(arg.getTenantId());
            captured.setDataType(arg.getDataType());
            return 1;
        });

        MetricDTO dto = MetricDTO.builder()
                .metricCode("ORDER_TOTAL_AMOUNT")
                .metricName("Order Total Amount")
                .description("Sum of order amounts")
                .metricType("BASE")
                .dataType("DECIMAL")
                .aggFunc("SUM")
                .unit("CNY")
                .format("#,##0.00")
                .build();

        MetricDefinition result = metricService.create(dto);

        assertNotNull(result);
        ArgumentCaptor<MetricDefinition> captor = ArgumentCaptor.forClass(MetricDefinition.class);
        verify(metricMapper, times(1)).insert(captor.capture());
        MetricDefinition persisted = captor.getValue();

        assertEquals(TENANT_ID.toString(), persisted.getTenantId());
        assertEquals("ORDER_TOTAL_AMOUNT", persisted.getMetricCode());
        assertEquals("Order Total Amount", persisted.getMetricName());
        assertEquals("BASE", persisted.getMetricType());
        assertEquals("DECIMAL", persisted.getDataType());
        assertEquals("SUM", persisted.getAggFunc());
        assertNull(persisted.getExpr());
        assertEquals("CNY", persisted.getUnit());
        assertEquals("#,##0.00", persisted.getFormat());
        assertNotNull(persisted.getCreateTime());
        assertNotNull(persisted.getUpdateTime());
        assertFalse(persisted.getDeleted());
        assertEquals(Integer.valueOf(1), persisted.getStatus());
        assertEquals(Integer.valueOf(0), persisted.getSortOrder());
        assertTrue(persisted.getVisible());
    }

    @Test
    void testCreateDerivedMetric() {
        when(metricMapper.insert(any(MetricDefinition.class))).thenAnswer(inv -> {
            MetricDefinition arg = inv.getArgument(0);
            arg.setId(UUID.randomUUID());
            return 1;
        });

        MetricDTO dto = MetricDTO.builder()
                .metricCode("ORDER_AVG_AMOUNT")
                .metricName("Order Average Amount")
                .metricType("DERIVED")
                .dataType("DECIMAL")
                .aggFunc("NONE")
                .expr("ORDER_TOTAL_AMOUNT / ORDER_COUNT")
                .build();

        metricService.create(dto);

        ArgumentCaptor<MetricDefinition> captor = ArgumentCaptor.forClass(MetricDefinition.class);
        verify(metricMapper).insert(captor.capture());
        MetricDefinition persisted = captor.getValue();
        assertEquals("DERIVED", persisted.getMetricType());
        assertEquals("ORDER_TOTAL_AMOUNT / ORDER_COUNT", persisted.getExpr());
        assertEquals("NONE", persisted.getAggFunc());
    }

    @Test
    void testCreateCompositeMetric() {
        when(metricMapper.insert(any(MetricDefinition.class))).thenAnswer(inv -> {
            MetricDefinition arg = inv.getArgument(0);
            arg.setId(UUID.randomUUID());
            return 1;
        });

        MetricDTO dto = MetricDTO.builder()
                .metricCode("GROSS_MARGIN")
                .metricName("Gross Margin")
                .metricType("COMPOSITE")
                .dataType("DECIMAL")
                .aggFunc("NONE")
                .expr("(REVENUE - COST) / REVENUE")
                .build();

        metricService.create(dto);

        ArgumentCaptor<MetricDefinition> captor = ArgumentCaptor.forClass(MetricDefinition.class);
        verify(metricMapper).insert(captor.capture());
        MetricDefinition persisted = captor.getValue();
        assertEquals("COMPOSITE", persisted.getMetricType());
        assertEquals("(REVENUE - COST) / REVENUE", persisted.getExpr());
        assertEquals("NONE", persisted.getAggFunc());
    }

    @Test
    void testCreateWithDuplicateCodeThrowsBusinessException() {
        when(metricMapper.insert(any(MetricDefinition.class)))
                .thenThrow(new DuplicateKeyException("uk_metric_tenant_code"));

        MetricDTO dto = MetricDTO.builder()
                .metricCode("DUPLICATE_CODE")
                .metricName("Duplicate")
                .metricType("BASE")
                .dataType("DECIMAL")
                .aggFunc("SUM")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> metricService.create(dto));
        assertTrue(ex.getMessage().contains("DUPLICATE_CODE"));
    }

    @Test
    void testUpdate() {
        UUID id = UUID.randomUUID();
        MetricDefinition existing = MetricDefinition.builder()
                .id(id)
                .tenantId(TENANT_ID.toString())
                .metricCode("ORDER_TOTAL_AMOUNT")
                .metricName("Order Total Amount")
                .metricType("BASE")
                .dataType("DECIMAL")
                .aggFunc("SUM")
                .build();
        when(metricMapper.selectById(id)).thenReturn(existing);
        when(metricMapper.updateById(any(MetricDefinition.class))).thenReturn(1);

        MetricDTO dto = MetricDTO.builder()
                .metricCode("ORDER_TOTAL_AMOUNT_V2")
                .metricName("Order Total Amount v2")
                .metricType("BASE")
                .dataType("DECIMAL")
                .aggFunc("SUM")
                .description("Updated description")
                .build();

        MetricDefinition result = metricService.update(id, dto);

        assertEquals("ORDER_TOTAL_AMOUNT_V2", result.getMetricCode());
        assertEquals("Order Total Amount v2", result.getMetricName());
        assertEquals("Updated description", result.getDescription());
        verify(metricMapper).updateById(any(MetricDefinition.class));
    }

    @Test
    void testUpdateNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(metricMapper.selectById(id)).thenReturn(null);

        MetricDTO dto = MetricDTO.builder()
                .metricCode("ANY")
                .metricName("Any")
                .metricType("BASE")
                .dataType("DECIMAL")
                .aggFunc("SUM")
                .build();

        assertThrows(BusinessException.class, () -> metricService.update(id, dto));
        verify(metricMapper, never()).updateById(any(MetricDefinition.class));
    }

    @Test
    void testDelete() {
        UUID id = UUID.randomUUID();
        when(metricMapper.deleteById(id)).thenReturn(1);

        metricService.delete(id);

        verify(metricMapper, times(1)).deleteById(id);
    }

    @Test
    void testDeleteNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(metricMapper.deleteById(id)).thenReturn(0);

        assertThrows(BusinessException.class, () -> metricService.delete(id));
    }

    @Test
    void testGetById() {
        UUID id = UUID.randomUUID();
        MetricDefinition entity = MetricDefinition.builder()
                .id(id)
                .tenantId(TENANT_ID.toString())
                .metricCode("M1")
                .metricType("BASE")
                .dataType("DECIMAL")
                .aggFunc("SUM")
                .build();
        when(metricMapper.selectById(id)).thenReturn(entity);

        MetricDefinition result = metricService.getById(id);

        assertNotNull(result);
        assertEquals("M1", result.getMetricCode());
    }

    @Test
    void testGetByCode() {
        MetricDefinition entity = MetricDefinition.builder()
                .tenantId(TENANT_ID.toString())
                .metricCode("ORDER_TOTAL_AMOUNT")
                .metricType("BASE")
                .dataType("DECIMAL")
                .aggFunc("SUM")
                .build();
        when(metricMapper.selectByTenantAndCode(eq(TENANT_ID.toString()), eq("ORDER_TOTAL_AMOUNT")))
                .thenReturn(entity);

        MetricDefinition result = metricService.getByCode("ORDER_TOTAL_AMOUNT");

        assertNotNull(result);
        assertEquals("ORDER_TOTAL_AMOUNT", result.getMetricCode());
        verify(metricMapper).selectByTenantAndCode(TENANT_ID.toString(), "ORDER_TOTAL_AMOUNT");
    }

    @Test
    void testListByTenant() {
        MetricDefinition m1 = MetricDefinition.builder()
                .tenantId(TENANT_ID.toString())
                .metricCode("M1")
                .metricType("BASE")
                .dataType("DECIMAL")
                .aggFunc("SUM")
                .build();
        MetricDefinition m2 = MetricDefinition.builder()
                .tenantId(TENANT_ID.toString())
                .metricCode("M2")
                .metricType("BASE")
                .dataType("DECIMAL")
                .aggFunc("COUNT")
                .build();

        Page<MetricDefinition> mapperPage = new Page<>(1, 10);
        mapperPage.setRecords(List.of(m1, m2));
        mapperPage.setTotal(2);
        when(metricMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mapperPage);

        Page<MetricDefinition> result = metricService.list(1, 10, null);

        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());
        assertEquals("M1", result.getRecords().get(0).getMetricCode());
        assertEquals("M2", result.getRecords().get(1).getMetricCode());
        verify(metricMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void testCreateRequiresTenantContext() {
        TenantContextHolder.clear();
        MetricDTO dto = MetricDTO.builder()
                .metricCode("M1")
                .metricName("Metric 1")
                .metricType("BASE")
                .dataType("DECIMAL")
                .aggFunc("SUM")
                .build();

        assertThrows(TenantParseException.class, () -> metricService.create(dto));
        verify(metricMapper, never()).insert(any(MetricDefinition.class));
    }

    @Test
    void testCreateRejectsMissingRequiredFields() {
        MetricDTO dto = MetricDTO.builder()
                .metricCode("")
                .metricName("")
                .metricType("BASE")
                .dataType("DECIMAL")
                .aggFunc("SUM")
                .build();

        assertThrows(BusinessException.class, () -> metricService.create(dto));
        verify(metricMapper, never()).insert(any(MetricDefinition.class));
    }

    @Test
    void testCreateRejectsInvalidMetricType() {
        MetricDTO dto = MetricDTO.builder()
                .metricCode("M1")
                .metricName("Metric 1")
                .metricType("INVALID")
                .dataType("DECIMAL")
                .aggFunc("SUM")
                .build();

        assertThrows(BusinessException.class, () -> metricService.create(dto));
        verify(metricMapper, never()).insert(any(MetricDefinition.class));
    }
}