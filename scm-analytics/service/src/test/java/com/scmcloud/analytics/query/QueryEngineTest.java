package com.scmcloud.analytics.query;

import com.scmcloud.analytics.api.dto.DatasetQueryRequest;
import com.scmcloud.analytics.api.dto.DatasetQueryResult;
import com.scmcloud.analytics.config.ClickHouseProperties;
import com.scmcloud.analytics.domain.entity.DatasetDefinition;
import com.scmcloud.analytics.domain.entity.DatasetFieldDefinition;
import com.scmcloud.analytics.domain.entity.MetricDefinition;
import com.scmcloud.analytics.mapper.MetricMapper;
import com.scmcloud.analytics.service.DatasetFieldService;
import com.scmcloud.analytics.service.DatasetService;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryEngineTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DATASET_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID METRIC_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock private DatasetService datasetService;
    @Mock private DatasetFieldService datasetFieldService;
    @Mock private MetricMapper metricMapper;
    @Mock private SqlGenerator sqlGenerator;
    @Mock private ClickHouseExecutor clickHouseExecutor;

    private QueryEngine queryEngine;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        ClickHouseProperties properties = new ClickHouseProperties();
        queryEngine = new QueryEngine(datasetService, datasetFieldService, metricMapper,
                sqlGenerator, clickHouseExecutor, properties);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void returnsRowsAndColumnMetadata() {
        DatasetDefinition dataset = dataset();
        DatasetFieldDefinition field = DatasetFieldDefinition.builder()
                .fieldCode("pay_amount")
                .fieldType("METRIC")
                .dataType("DECIMAL")
                .metricId(METRIC_ID)
                .build();
        MetricDefinition metric = MetricDefinition.builder()
                .id(METRIC_ID)
                .metricCode("PAY_AMOUNT")
                .metricType("BASE")
                .build();
        DatasetQueryRequest request = new DatasetQueryRequest(
                "sales", List.of("order_date"), List.of("pay_amount"),
                List.of(), List.of(), 10);

        when(datasetService.getByCode("sales")).thenReturn(dataset);
        when(datasetFieldService.listByDataset(DATASET_ID)).thenReturn(List.of(field));
        when(metricMapper.selectById(METRIC_ID)).thenReturn(metric);
        when(metricMapper.selectVisibleByTenant(TENANT_ID.toString())).thenReturn(List.of(metric));
        when(sqlGenerator.generate(eq(dataset), anyList(), anyList(), eq(request), any(QueryContext.class)))
                .thenReturn(new GeneratedQuery("SELECT 1", List.of("order_date", "pay_amount"),
                        List.of("DATE", "DECIMAL"), 10));
        when(clickHouseExecutor.query(anyString(), anyList(), eq(30)))
                .thenReturn(List.of(Map.of("order_date", "2026-10-23", "pay_amount", 42)));

        DatasetQueryResult result = queryEngine.query(request);

        assertEquals("sales", result.datasetCode());
        assertEquals(2, result.columns().size());
        assertEquals("DECIMAL", result.columns().get(1).dataType());
        assertEquals(1, result.rowCount());
        assertEquals(10, result.limit());
        verify(clickHouseExecutor).query("SELECT 1", List.of("order_date", "pay_amount"), 30);
    }

    @Test
    void rejectsUnknownDataset() {
        DatasetQueryRequest request = new DatasetQueryRequest(
                "missing", List.of("order_date"), List.of(), List.of(), List.of(), 10);
        when(datasetService.getByCode("missing")).thenReturn(null);

        assertThrows(BusinessException.class, () -> queryEngine.query(request));
        verify(clickHouseExecutor, never()).query(anyString(), anyList(), eq(30));
    }

    @Test
    void requiresTenantContext() {
        TenantContextHolder.clear();
        DatasetQueryRequest request = new DatasetQueryRequest(
                "sales", List.of("order_date"), List.of(), List.of(), List.of(), 10);

        assertThrows(RuntimeException.class, () -> queryEngine.query(request));
    }

    private DatasetDefinition dataset() {
        return DatasetDefinition.builder()
                .id(DATASET_ID)
                .datasetCode("sales")
                .sourceType("CLICKHOUSE")
                .sourceTable("ods_order")
                .enabled(true)
                .status(1)
                .build();
    }
}
