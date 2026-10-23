package com.scmcloud.analytics.query;

import com.scmcloud.analytics.api.dto.DatasetQueryRequest;
import com.scmcloud.analytics.config.ClickHouseProperties;
import com.scmcloud.analytics.domain.entity.DatasetDefinition;
import com.scmcloud.analytics.domain.entity.DatasetFieldDefinition;
import com.scmcloud.analytics.domain.entity.MetricDefinition;
import com.scmcloud.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlGeneratorTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID M_PAY = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID M_COUNT = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID M_AVG = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private SqlGenerator generator;
    private DatasetDefinition dataset;
    private List<DatasetFieldDefinition> fields;
    private List<MetricDefinition> metrics;

    @BeforeEach
    void setUp() {
        ClickHouseProperties properties = new ClickHouseProperties();
        properties.setDatabase("analytics");
        generator = new SqlGenerator(properties, new PermissionSqlInjector());

        dataset = DatasetDefinition.builder()
                .datasetCode("sales")
                .sourceType("CLICKHOUSE")
                .sourceTable("ods_order")
                .enabled(true)
                .status(1)
                .build();

        fields = List.of(
                dimension("order_date", "toDate(create_time)", "DATE"),
                dimension("category_id", null, "VARCHAR"),
                dimension("order_status", null, "VARCHAR"),
                metricField("pay_amount", M_PAY, "DECIMAL"),
                metricField("order_cnt", M_COUNT, "BIGINT"),
                metricField("avg_order_value", M_AVG, "DECIMAL"),
                DatasetFieldDefinition.builder()
                        .fieldCode("secret")
                        .fieldType("DIMENSION")
                        .dataType("VARCHAR")
                        .isHidden(true)
                        .build()
        );

        metrics = List.of(
                MetricDefinition.builder()
                        .id(M_PAY)
                        .metricCode("PAY_AMOUNT")
                        .metricType("BASE")
                        .aggFunc("SUM")
                        .expr("pay_amount")
                        .dataType("DECIMAL")
                        .build(),
                MetricDefinition.builder()
                        .id(M_COUNT)
                        .metricCode("ORDER_COUNT")
                        .metricType("BASE")
                        .aggFunc("COUNT")
                        .expr("order_no")
                        .dataType("BIGINT")
                        .build(),
                MetricDefinition.builder()
                        .id(M_AVG)
                        .metricCode("AVG_ORDER_VALUE")
                        .metricType("DERIVED")
                        .aggFunc("NONE")
                        .expr("PAY_AMOUNT / ORDER_COUNT")
                        .dataType("DECIMAL")
                        .build()
        );
    }

    @Test
    void generatesTenantScopedGroupedQuery() {
        DatasetQueryRequest request = new DatasetQueryRequest(
                "sales",
                List.of("order_date", "category_id"),
                List.of("pay_amount", "order_cnt"),
                List.of(new DatasetQueryRequest.Filter("order_status", "EQ", List.of("PAID"))),
                List.of(),
                50);

        GeneratedQuery query = generator.generate(dataset, fields, metrics, request, context());

        assertTrue(query.sql().startsWith("SELECT "));
        assertTrue(query.sql().contains("toDate(create_time) AS `order_date`"));
        assertTrue(query.sql().contains("category_id AS `category_id`"));
        assertTrue(query.sql().contains("(sum(pay_amount)) AS `pay_amount`"));
        assertTrue(query.sql().contains("(count(order_no)) AS `order_cnt`"));
        assertTrue(query.sql().contains("FROM analytics.ods_order"));
        assertTrue(query.sql().contains("tenant_id = '" + TENANT_ID + "'"));
        assertTrue(query.sql().contains("order_status = 'PAID'"));
        assertTrue(query.sql().contains("GROUP BY toDate(create_time), category_id"));
        assertTrue(query.sql().contains("ORDER BY `order_date` ASC, `category_id` ASC"));
        assertTrue(query.sql().endsWith("LIMIT 50"));
        assertEquals(List.of("order_date", "category_id", "pay_amount", "order_cnt"), query.columns());
        assertEquals(List.of("DATE", "VARCHAR", "DECIMAL", "BIGINT"), query.dataTypes());
    }

    @Test
    void substitutesDerivedMetricReferences() {
        DatasetQueryRequest request = new DatasetQueryRequest(
                "sales",
                List.of("category_id"),
                List.of("avg_order_value"),
                List.of(),
                List.of(),
                10);

        GeneratedQuery query = generator.generate(dataset, fields, metrics, request, context());

        assertTrue(query.sql().contains("(sum(pay_amount) / count(order_no)) AS `avg_order_value`"));
    }

    @Test
    void rejectsFieldOutsideDataset() {
        DatasetQueryRequest request = new DatasetQueryRequest(
                "sales", List.of("unknown_field"), List.of(), List.of(), List.of(), 10);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> generator.generate(dataset, fields, metrics, request, context()));
        assertTrue(ex.getMessage().contains("unknown_field"));
    }

    @Test
    void rejectsHiddenField() {
        DatasetQueryRequest request = new DatasetQueryRequest(
                "sales", List.of("secret"), List.of(), List.of(), List.of(), 10);

        assertThrows(BusinessException.class,
                () -> generator.generate(dataset, fields, metrics, request, context()));
    }

    @Test
    void rejectsMetricUsedAsDimension() {
        DatasetQueryRequest request = new DatasetQueryRequest(
                "sales", List.of("pay_amount"), List.of(), List.of(), List.of(), 10);

        assertThrows(BusinessException.class,
                () -> generator.generate(dataset, fields, metrics, request, context()));
    }

    @Test
    void rejectsUnsafeSourceTable() {
        dataset.setSourceTable("ods_order; DROP TABLE x");

        DatasetQueryRequest request = new DatasetQueryRequest(
                "sales", List.of("order_date"), List.of(), List.of(), List.of(), 10);

        assertThrows(BusinessException.class,
                () -> generator.generate(dataset, fields, metrics, request, context()));
    }

    @Test
    void capsLimitAtMaximum() {
        DatasetQueryRequest request = new DatasetQueryRequest(
                "sales", List.of("order_date"), List.of(), List.of(), List.of(), 1_000_000);

        GeneratedQuery query = generator.generate(dataset, fields, metrics, request, context());

        assertTrue(query.sql().endsWith("LIMIT " + SqlGenerator.MAX_LIMIT));
        assertEquals(SqlGenerator.MAX_LIMIT, query.limit());
    }

    @Test
    void escapesStringFilterValues() {
        DatasetQueryRequest request = new DatasetQueryRequest(
                "sales",
                List.of("order_date"),
                List.of(),
                List.of(new DatasetQueryRequest.Filter("category_id", "EQ", List.of("O'Reilly"))),
                List.of(),
                10);

        GeneratedQuery query = generator.generate(dataset, fields, metrics, request, context());

        assertTrue(query.sql().contains("category_id = 'O''Reilly'"));
    }

    @Test
    void rejectsUnsupportedOperator() {
        DatasetQueryRequest request = new DatasetQueryRequest(
                "sales",
                List.of("order_date"),
                List.of(),
                List.of(new DatasetQueryRequest.Filter("category_id", "DROP", List.of("x"))),
                List.of(),
                10);

        assertThrows(BusinessException.class,
                () -> generator.generate(dataset, fields, metrics, request, context()));
    }

    @Test
    void injectsOrganizationScopeWhenDatasetSupportsIt() {
        List<DatasetFieldDefinition> withOrg = new java.util.ArrayList<>(fields);
        withOrg.add(dimension("organization_id", null, "VARCHAR"));
        QueryContext scoped = new QueryContext(TENANT_ID, List.of("org-1", "org-2"));

        DatasetQueryRequest request = new DatasetQueryRequest(
                "sales", List.of("order_date"), List.of(), List.of(), List.of(), 10);

        GeneratedQuery query = generator.generate(dataset, withOrg, metrics, request, scoped);

        assertTrue(query.sql().contains("organization_id IN ('org-1', 'org-2')"));
    }

    @Test
    void rejectsOrganizationScopeWhenDatasetHasNoOrganizationField() {
        QueryContext scoped = new QueryContext(TENANT_ID, List.of("org-1"));
        DatasetQueryRequest request = new DatasetQueryRequest(
                "sales", List.of("order_date"), List.of(), List.of(), List.of(), 10);

        assertThrows(BusinessException.class,
                () -> generator.generate(dataset, fields, metrics, request, scoped));
    }

    @Test
    void rejectsCircularDerivedMetrics() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        List<MetricDefinition> circular = List.of(
                MetricDefinition.builder().id(first).metricCode("A").metricType("DERIVED")
                        .aggFunc("NONE").expr("B + 1").dataType("DECIMAL").build(),
                MetricDefinition.builder().id(second).metricCode("B").metricType("DERIVED")
                        .aggFunc("NONE").expr("A + 1").dataType("DECIMAL").build());
        List<DatasetFieldDefinition> circularFields = List.of(
                metricField("a_metric", first, "DECIMAL"));

        DatasetQueryRequest request = new DatasetQueryRequest(
                "sales", List.of(), List.of("a_metric"), List.of(), List.of(), 10);

        assertThrows(BusinessException.class,
                () -> generator.generate(dataset, circularFields, circular, request, context()));
    }

    private QueryContext context() {
        return new QueryContext(TENANT_ID, List.of());
    }

    private DatasetFieldDefinition dimension(String code, String expr, String dataType) {
        return DatasetFieldDefinition.builder()
                .fieldCode(code)
                .fieldType("DIMENSION")
                .dataType(dataType)
                .expr(expr)
                .isHidden(false)
                .build();
    }

    private DatasetFieldDefinition metricField(String code, UUID metricId, String dataType) {
        return DatasetFieldDefinition.builder()
                .fieldCode(code)
                .fieldType("METRIC")
                .dataType(dataType)
                .metricId(metricId)
                .isHidden(false)
                .build();
    }
}
