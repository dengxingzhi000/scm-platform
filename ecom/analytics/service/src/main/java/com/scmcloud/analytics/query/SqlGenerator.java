package com.scmcloud.analytics.query;

import com.scmcloud.analytics.api.dto.DatasetQueryRequest;
import com.scmcloud.analytics.config.ClickHouseProperties;
import com.scmcloud.analytics.domain.entity.DatasetDefinition;
import com.scmcloud.analytics.domain.entity.DatasetFieldDefinition;
import com.scmcloud.analytics.domain.entity.MetricDefinition;
import com.scmcloud.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Translates a {@link DatasetQueryRequest} into ClickHouse SQL.
 *
 * <p>Security invariants:
 * <ul>
 *   <li>Every requested dimension, metric, filter and order-by field must be
 *       present in the dataset's field metadata.</li>
 *   <li>Identifiers and expressions are validated; values are serialized with
 *       type-aware escaping.</li>
 *   <li>Every query carries a {@code tenant_id} predicate; organization scope
 *       is added by {@link PermissionSqlInjector} when applicable.</li>
 *   <li>Result size is capped at {@value #MAX_LIMIT} rows.</li>
 * </ul>
 */
@Component
public class SqlGenerator {

    public static final int DEFAULT_LIMIT = 1_000;
    public static final int MAX_LIMIT = 10_000;
    private static final int MAX_IN_VALUES = 1_000;

    private static final Set<String> OPERATORS = Set.of(
            "EQ", "NE", "GT", "GTE", "LT", "LTE", "IN", "NOT_IN", "BETWEEN",
            "LIKE", "NOT_LIKE", "IS_NULL", "IS_NOT_NULL");

    private static final Pattern TOKEN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final ClickHouseProperties properties;
    private final PermissionSqlInjector permissionSqlInjector;

    public SqlGenerator(ClickHouseProperties properties, PermissionSqlInjector permissionSqlInjector) {
        this.properties = properties;
        this.permissionSqlInjector = permissionSqlInjector;
    }

    public GeneratedQuery generate(DatasetDefinition dataset,
                                   List<DatasetFieldDefinition> fields,
                                   List<MetricDefinition> metrics,
                                   DatasetQueryRequest request,
                                   QueryContext context) {
        validateRequest(dataset, request, context);

        Map<String, DatasetFieldDefinition> fieldsByCode = fields.stream()
                .collect(Collectors.toMap(
                        DatasetFieldDefinition::getFieldCode,
                        field -> field,
                        (left, right) -> left,
                        LinkedHashMap::new));
        Map<UUID, MetricDefinition> metricsById = metrics.stream()
                .filter(metric -> metric.getId() != null)
                .collect(Collectors.toMap(MetricDefinition::getId, metric -> metric, (left, right) -> left));
        Map<String, MetricDefinition> metricsByCode = metrics.stream()
                .filter(metric -> metric.getMetricCode() != null)
                .collect(Collectors.toMap(MetricDefinition::getMetricCode, metric -> metric, (left, right) -> left));

        List<SelectedField> dimensions = resolveDimensions(request.dimensions(), fieldsByCode);
        BaseMetricResolver resolver = new BaseMetricResolver(fieldsByCode, metricsById, metricsByCode);
        List<SelectedField> selectedMetrics = resolveMetrics(request.metrics(), fieldsByCode, resolver);

        String source = resolveSourceTable(dataset);
        StringBuilder select = new StringBuilder("SELECT ");
        List<String> selectParts = new ArrayList<>();
        for (SelectedField dimension : dimensions) {
            selectParts.add(dimension.expression() + " AS " + SqlSafety.quoteIdentifier(dimension.code()));
        }
        for (SelectedField metric : selectedMetrics) {
            selectParts.add("(" + metric.expression() + ") AS " + SqlSafety.quoteIdentifier(metric.code()));
        }
        select.append(String.join(", ", selectParts));
        select.append(" FROM ").append(source);

        List<String> predicates = new ArrayList<>();
        predicates.add(permissionSqlInjector.tenantPredicate(context.tenantId()));
        String organizationPredicate = permissionSqlInjector.organizationPredicate(context.organizationIds());
        if (organizationPredicate != null) {
            if (!fieldsByCode.containsKey("organization_id")) {
                throw new BusinessException("Dataset does not support organization-scoped queries: "
                        + dataset.getDatasetCode());
            }
            predicates.add(organizationPredicate);
        }
        for (DatasetQueryRequest.Filter filter : request.filters()) {
            predicates.add(filterPredicate(filter, fieldsByCode));
        }
        select.append(" WHERE ").append(String.join(" AND ", predicates));

        if (!dimensions.isEmpty()) {
            select.append(" GROUP BY ")
                    .append(dimensions.stream()
                            .map(SelectedField::expression)
                            .collect(Collectors.joining(", ")));
        }

        List<String> outputCodes = new ArrayList<>();
        dimensions.forEach(field -> outputCodes.add(field.code()));
        selectedMetrics.forEach(field -> outputCodes.add(field.code()));
        select.append(" ORDER BY ").append(orderByClause(request, outputCodes, dimensions, selectedMetrics));

        int limit = normalizeLimit(request.limit());
        select.append(" LIMIT ").append(limit);

        List<String> dataTypes = new ArrayList<>();
        dimensions.forEach(field -> dataTypes.add(field.dataType()));
        selectedMetrics.forEach(field -> dataTypes.add(field.dataType()));

        return new GeneratedQuery(select.toString(), outputCodes, dataTypes, limit);
    }

    private void validateRequest(DatasetDefinition dataset, DatasetQueryRequest request, QueryContext context) {
        if (dataset == null) {
            throw new BusinessException("Dataset not found");
        }
        if (Boolean.FALSE.equals(dataset.getEnabled())) {
            throw new BusinessException("Dataset is disabled: " + dataset.getDatasetCode());
        }
        if (dataset.getStatus() != null && dataset.getStatus() == 0) {
            throw new BusinessException("Dataset is disabled: " + dataset.getDatasetCode());
        }
        if (!"CLICKHOUSE".equalsIgnoreCase(dataset.getSourceType())) {
            throw new BusinessException("Unsupported dataset source type: " + dataset.getSourceType());
        }
        if (request == null) {
            throw new BusinessException("Query request must not be null");
        }
        if (context == null || context.tenantId() == null) {
            throw new BusinessException("Tenant context is required for analytics queries");
        }
        if (request.dimensions().isEmpty() && request.metrics().isEmpty()) {
            throw new BusinessException("At least one dimension or metric is required");
        }
    }

    public String resolveSourceTable(DatasetDefinition dataset) {
        String table = SqlSafety.requireTable(dataset.getSourceTable());
        if (table.contains(".")) {
            return table;
        }
        String database = properties.getDatabase();
        if (database == null || database.isBlank()) {
            database = "analytics";
        }
        SqlSafety.requireIdentifier(database, "ClickHouse database");
        return database + "." + table;
    }

    private List<SelectedField> resolveDimensions(List<String> codes,
                                                  Map<String, DatasetFieldDefinition> fieldsByCode) {
        List<SelectedField> result = new ArrayList<>();
        for (String code : new LinkedHashSet<>(codes)) {
            DatasetFieldDefinition field = requireField(code, fieldsByCode);
            String type = normalizeType(field.getFieldType());
            if (!"DIMENSION".equals(type) && !"CALCULATED".equals(type)) {
                throw new BusinessException("Field is not a dimension: " + code);
            }
            String expression = firstNonBlank(field.getExpr(), field.getFieldCode());
            result.add(new SelectedField(
                    SqlSafety.requireIdentifier(field.getFieldCode(), "field code"),
                    SqlSafety.requireExpression(expression, "dimension " + code),
                    field.getDataType() == null ? "VARCHAR" : field.getDataType()));
        }
        return result;
    }

    private List<SelectedField> resolveMetrics(List<String> codes,
                                               Map<String, DatasetFieldDefinition> fieldsByCode,
                                               BaseMetricResolver resolver) {
        List<SelectedField> result = new ArrayList<>();
        for (String code : new LinkedHashSet<>(codes)) {
            DatasetFieldDefinition field = requireField(code, fieldsByCode);
            if (!"METRIC".equals(normalizeType(field.getFieldType()))) {
                throw new BusinessException("Field is not a metric: " + code);
            }
            MetricDefinition metric = resolver.metricFor(field);
            String expression = resolver.resolve(metric, field);
            result.add(new SelectedField(
                    SqlSafety.requireIdentifier(field.getFieldCode(), "field code"),
                    expression,
                    metric.getDataType() == null ? field.getDataType() : metric.getDataType()));
        }
        return result;
    }

    private DatasetFieldDefinition requireField(String code, Map<String, DatasetFieldDefinition> fieldsByCode) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Field code must not be empty");
        }
        DatasetFieldDefinition field = fieldsByCode.get(code);
        if (field == null) {
            throw new BusinessException("Field is not part of this dataset: " + code);
        }
        if (Boolean.TRUE.equals(field.getIsHidden())) {
            throw new BusinessException("Field is not queryable: " + code);
        }
        return field;
    }

    private String filterPredicate(DatasetQueryRequest.Filter filter,
                                   Map<String, DatasetFieldDefinition> fieldsByCode) {
        if (filter == null || filter.field() == null || filter.field().isBlank()) {
            throw new BusinessException("Filter field must not be empty");
        }
        DatasetFieldDefinition field = requireField(filter.field(), fieldsByCode);
        String operator = filter.operator() == null ? "" : filter.operator().toUpperCase(Locale.ROOT);
        if (!OPERATORS.contains(operator)) {
            throw new BusinessException("Unsupported filter operator: " + filter.operator());
        }
        String expression = SqlSafety.requireExpression(
                firstNonBlank(field.getExpr(), field.getFieldCode()),
                "filter " + filter.field());
        List<String> values = filter.values();
        if (values.size() > MAX_IN_VALUES) {
            throw new BusinessException("Too many filter values for field: " + filter.field());
        }

        return switch (operator) {
            case "IS_NULL" -> expression + " IS NULL";
            case "IS_NOT_NULL" -> expression + " IS NOT NULL";
            case "EQ" -> expression + " = " + oneValue(field, values);
            case "NE" -> expression + " != " + oneValue(field, values);
            case "GT" -> expression + " > " + oneValue(field, values);
            case "GTE" -> expression + " >= " + oneValue(field, values);
            case "LT" -> expression + " < " + oneValue(field, values);
            case "LTE" -> expression + " <= " + oneValue(field, values);
            case "LIKE" -> expression + " LIKE " + SqlSafety.quoteString(requiredValue(values, 0));
            case "NOT_LIKE" -> expression + " NOT LIKE " + SqlSafety.quoteString(requiredValue(values, 0));
            case "IN" -> expression + " IN (" + valueList(field, values) + ")";
            case "NOT_IN" -> expression + " NOT IN (" + valueList(field, values) + ")";
            case "BETWEEN" -> {
                if (values.size() != 2) {
                    throw new BusinessException("BETWEEN requires exactly two values");
                }
                yield expression + " BETWEEN " + SqlSafety.literal(field.getDataType(), values.get(0))
                        + " AND " + SqlSafety.literal(field.getDataType(), values.get(1));
            }
            default -> throw new BusinessException("Unsupported filter operator: " + operator);
        };
    }

    private String oneValue(DatasetFieldDefinition field, List<String> values) {
        if (values.size() != 1) {
            throw new BusinessException("Filter requires exactly one value for field: " + field.getFieldCode());
        }
        return SqlSafety.literal(field.getDataType(), values.get(0));
    }

    private String valueList(DatasetFieldDefinition field, List<String> values) {
        if (values.isEmpty()) {
            throw new BusinessException("Filter requires at least one value for field: " + field.getFieldCode());
        }
        return values.stream()
                .map(value -> SqlSafety.literal(field.getDataType(), value))
                .collect(Collectors.joining(", "));
    }

    private String requiredValue(List<String> values, int index) {
        if (values.size() <= index || values.get(index) == null) {
            throw new BusinessException("Filter value is required");
        }
        return values.get(index);
    }

    private String orderByClause(DatasetQueryRequest request,
                                 List<String> outputCodes,
                                 List<SelectedField> dimensions,
                                 List<SelectedField> metrics) {
        if (request.orderBy().isEmpty()) {
            if (!dimensions.isEmpty()) {
                return dimensions.stream()
                        .map(field -> SqlSafety.quoteIdentifier(field.code()) + " ASC")
                        .collect(Collectors.joining(", "));
            }
            return SqlSafety.quoteIdentifier(metrics.get(0).code()) + " ASC";
        }

        Set<String> allowed = Set.copyOf(outputCodes);
        List<String> parts = new ArrayList<>();
        for (DatasetQueryRequest.OrderBy order : request.orderBy()) {
            if (order == null || order.field() == null || !allowed.contains(order.field())) {
                throw new BusinessException("Order by field must be a requested dimension or metric: "
                        + (order == null ? null : order.field()));
            }
            String direction = order.direction() == null ? "ASC" : order.direction().toUpperCase(Locale.ROOT);
            if (!"ASC".equals(direction) && !"DESC".equals(direction)) {
                throw new BusinessException("Order direction must be ASC or DESC");
            }
            parts.add(SqlSafety.quoteIdentifier(order.field()) + " " + direction);
        }
        return String.join(", ", parts);
    }

    private int normalizeLimit(Integer requested) {
        if (requested == null) {
            return DEFAULT_LIMIT;
        }
        if (requested < 1) {
            throw new BusinessException("Limit must be greater than zero");
        }
        return Math.min(requested, MAX_LIMIT);
    }

    private static String normalizeType(String type) {
        return type == null ? "" : type.toUpperCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record SelectedField(String code, String expression, String dataType) {
    }

    /**
     * Resolves metric definitions into aggregate SQL, substituting referenced
     * metric codes for derived / composite metrics.
     */
    private static final class BaseMetricResolver {

        private final Map<String, DatasetFieldDefinition> fieldsByCode;
        private final Map<UUID, MetricDefinition> metricsById;
        private final Map<String, MetricDefinition> metricsByCode;
        private final Map<String, String> aggregateCache = new LinkedHashMap<>();

        private BaseMetricResolver(Map<String, DatasetFieldDefinition> fieldsByCode,
                                   Map<UUID, MetricDefinition> metricsById,
                                   Map<String, MetricDefinition> metricsByCode) {
            this.fieldsByCode = fieldsByCode;
            this.metricsById = metricsById;
            this.metricsByCode = metricsByCode;
        }

        private MetricDefinition metricFor(DatasetFieldDefinition field) {
            MetricDefinition metric = field.getMetricId() == null ? null : metricsById.get(field.getMetricId());
            if (metric == null) {
                throw new BusinessException("Metric definition not found for field: " + field.getFieldCode());
            }
            return metric;
        }

        private String resolve(MetricDefinition metric, DatasetFieldDefinition field) {
            String type = normalizeType(metric.getMetricType());
            if (type.isEmpty() || "BASE".equals(type)) {
                return aggregateExpression(metric, field);
            }
            String formula = firstNonBlank(metric.getExpr(), field.getExpr());
            if (formula == null) {
                throw new BusinessException("Expression is required for metric: " + metric.getMetricCode());
            }
            String safeFormula = SqlSafety.requireExpression(formula, "metric " + metric.getMetricCode());
            return substituteReferences(safeFormula, new ArrayDeque<>());
        }

        private String aggregateExpression(MetricDefinition metric, DatasetFieldDefinition field) {
            String cached = aggregateCache.get(metric.getMetricCode());
            if (cached != null) {
                return cached;
            }
            String expression = firstNonBlank(metric.getExpr(), field == null ? null : field.getExpr(),
                    field == null ? null : field.getFieldCode(), metric.getMetricCode());
            String safeExpression = SqlSafety.requireExpression(expression, "metric " + metric.getMetricCode());
            String agg = metric.getAggFunc() == null ? "NONE" : metric.getAggFunc().toUpperCase(Locale.ROOT);
            if (!SqlSafety.AGG_FUNCTIONS.contains(agg)) {
                throw new BusinessException("Unsupported aggregate function: " + metric.getAggFunc());
            }
            String sql = switch (agg) {
                case "NONE" -> safeExpression;
                case "COUNT_DISTINCT" -> "count(DISTINCT " + safeExpression + ")";
                default -> agg.toLowerCase(Locale.ROOT) + "(" + safeExpression + ")";
            };
            if (metric.getMetricCode() != null) {
                aggregateCache.put(metric.getMetricCode(), sql);
            }
            return sql;
        }

        private String substituteReferences(String formula, Deque<String> stack) {
            Matcher matcher = TOKEN.matcher(formula);
            StringBuilder result = new StringBuilder(formula.length());
            int cursor = 0;
            while (matcher.find()) {
                result.append(formula, cursor, matcher.start());
                String token = matcher.group();
                int next = matcher.end();
                while (next < formula.length() && Character.isWhitespace(formula.charAt(next))) {
                    next++;
                }
                boolean functionCall = next < formula.length() && formula.charAt(next) == '(';
                MetricDefinition referenced = functionCall ? null : metricsByCode.get(token);
                if (referenced == null) {
                    result.append(token);
                } else {
                    result.append(resolveReference(token, referenced, stack));
                }
                cursor = matcher.end();
            }
            result.append(formula.substring(cursor));
            return result.toString();
        }

        private String resolveReference(String code, MetricDefinition metric, Deque<String> stack) {
            if (stack.contains(code)) {
                throw new BusinessException("Circular metric reference: " + String.join(" -> ", stack) + " -> " + code);
            }
            stack.addLast(code);
            try {
                String type = normalizeType(metric.getMetricType());
                if (type.isEmpty() || "BASE".equals(type)) {
                    DatasetFieldDefinition field = fieldsByCode.values().stream()
                            .filter(candidate -> Objects.equals(candidate.getMetricId(), metric.getId()))
                            .findFirst()
                            .orElse(null);
                    return aggregateExpression(metric, field);
                }
                String formula = firstNonBlank(metric.getExpr());
                if (formula == null) {
                    throw new BusinessException("Expression is required for metric: " + code);
                }
                return substituteReferences(SqlSafety.requireExpression(formula, "metric " + code), stack);
            } finally {
                stack.removeLast();
            }
        }
    }
}
