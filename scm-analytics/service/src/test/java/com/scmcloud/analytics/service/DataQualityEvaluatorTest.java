package com.scmcloud.analytics.service;

import com.scmcloud.analytics.config.ClickHouseProperties;
import com.scmcloud.analytics.domain.entity.DataQualityRule;
import com.scmcloud.analytics.domain.entity.DatasetDefinition;
import com.scmcloud.analytics.domain.entity.DatasetFieldDefinition;
import com.scmcloud.analytics.query.ClickHouseExecutor;
import com.scmcloud.analytics.query.SqlGenerator;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataQualityEvaluatorTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DATASET_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock private DatasetService datasetService;
    @Mock private DatasetFieldService datasetFieldService;
    @Mock private SqlGenerator sqlGenerator;
    @Mock private ClickHouseExecutor clickHouseExecutor;

    private DataQualityEvaluator evaluator;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        ClickHouseProperties properties = new ClickHouseProperties();
        evaluator = new DataQualityEvaluator(datasetService, datasetFieldService,
                sqlGenerator, clickHouseExecutor, properties);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void notNullRuleFailsWhenViolationsExist() {
        stubDataset("order_no", "order_no");
        when(clickHouseExecutor.query(anyString(), eq(List.of("violations")), eq(30)))
                .thenReturn(List.of(Map.of("violations", 3L)));

        DataQualityEvaluator.QualityCheckResult result = evaluator.evaluate(rule("NOT_NULL", null));

        assertFalse(result.passed());
        assertEquals(0, new BigDecimal("3").compareTo(result.metricValue()));
        assertTrue(result.message().contains("3"));
    }

    @Test
    void rangeRuleUsesConfiguredBounds() {
        stubDataset("pay_amount", "pay_amount");
        when(clickHouseExecutor.query(anyString(), anyList(), eq(30)))
                .thenReturn(List.of(Map.of("violations", 0L)));

        DataQualityEvaluator.QualityCheckResult result = evaluator.evaluate(
                rule("RANGE", Map.of("min", 0, "max", 100000)));

        assertTrue(result.passed());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.metricValue()));
    }

    @Test
    void freshnessRuleDefaultsTo24Hours() {
        stubDataset("create_time", "create_time");
        when(clickHouseExecutor.query(anyString(), anyList(), eq(30)))
                .thenReturn(List.of(Map.of("violations", 1L)));

        DataQualityEvaluator.QualityCheckResult result = evaluator.evaluate(rule("FRESHNESS", null));

        assertFalse(result.passed());
    }

    @Test
    void rejectsUnknownTargetField() {
        DatasetDefinition dataset = dataset();
        when(datasetService.getByCode("sales")).thenReturn(dataset);
        when(datasetFieldService.listByDataset(DATASET_ID)).thenReturn(List.of());

        assertThrows(BusinessException.class,
                () -> evaluator.evaluate(rule("NOT_NULL", null)));
    }

    @Test
    void rejectsUnsupportedRuleType() {
        stubDataset("pay_amount", "pay_amount");

        assertThrows(BusinessException.class,
                () -> evaluator.evaluate(rule("CUSTOM_SQL", null)));
    }

    @Test
    void rangeRuleRequiresBounds() {
        stubDataset("pay_amount", "pay_amount");

        assertThrows(BusinessException.class,
                () -> evaluator.evaluate(rule("RANGE", Map.of())));
    }

    private void stubDataset(String fieldCode, String expr) {
        DatasetDefinition dataset = dataset();
        DatasetFieldDefinition field = DatasetFieldDefinition.builder()
                .fieldCode(fieldCode)
                .fieldType("DIMENSION")
                .dataType("DECIMAL")
                .expr(expr)
                .build();
        when(datasetService.getByCode("sales")).thenReturn(dataset);
        when(datasetFieldService.listByDataset(DATASET_ID)).thenReturn(List.of(field));
        when(sqlGenerator.resolveSourceTable(dataset)).thenReturn("analytics.ods_order");
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

    private DataQualityRule rule(String type, Map<String, Object> config) {
        return DataQualityRule.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID.toString())
                .ruleCode("RULE_" + type)
                .datasetCode("sales")
                .ruleType(type)
                .targetField(targetField(type))
                .config(config)
                .enabled(true)
                .status(1)
                .build();
    }

    private String targetField(String type) {
        return switch (type) {
            case "FRESHNESS" -> "create_time";
            case "NOT_NULL" -> "order_no";
            default -> "pay_amount";
        };
    }
}
