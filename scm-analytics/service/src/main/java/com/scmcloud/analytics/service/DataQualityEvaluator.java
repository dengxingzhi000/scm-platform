package com.scmcloud.analytics.service;

import com.scmcloud.analytics.config.ClickHouseProperties;
import com.scmcloud.analytics.domain.entity.DataQualityRule;
import com.scmcloud.analytics.domain.entity.DatasetDefinition;
import com.scmcloud.analytics.domain.entity.DatasetFieldDefinition;
import com.scmcloud.analytics.query.ClickHouseExecutor;
import com.scmcloud.analytics.query.SqlGenerator;
import com.scmcloud.analytics.query.SqlSafety;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Evaluates one quality rule against ClickHouse and returns a check result.
 *
 * <p>Rules operate on dataset metadata, so the same identifier validation used
 * by the query engine applies here. No rule can inject raw SQL.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataQualityEvaluator {

    private static final int DEFAULT_FRESHNESS_HOURS = 24;

    private final DatasetService datasetService;
    private final DatasetFieldService datasetFieldService;
    private final SqlGenerator sqlGenerator;
    private final ClickHouseExecutor clickHouseExecutor;
    private final ClickHouseProperties properties;

    public QualityCheckResult evaluate(DataQualityRule rule) {
        if (rule == null) {
            throw new BusinessException("Quality rule must not be null");
        }
        DatasetDefinition dataset = datasetService.getByCode(rule.getDatasetCode());
        if (dataset == null) {
            throw new BusinessException("Dataset not found: " + rule.getDatasetCode());
        }
        List<DatasetFieldDefinition> fields = datasetFieldService.listByDataset(dataset.getId());
        DatasetFieldDefinition field = fields.stream()
                .filter(candidate -> candidate.getFieldCode().equals(rule.getTargetField()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Quality target field is not part of dataset: " + rule.getTargetField()));

        String table = sqlGenerator.resolveSourceTable(dataset);
        String expression = SqlSafety.requireExpression(
                field.getExpr() == null || field.getExpr().isBlank()
                        ? field.getFieldCode() : field.getExpr(),
                "quality field " + rule.getTargetField());
        String tenantFilter = "tenant_id = " + SqlSafety.quoteString(
                TenantContextHolder.getRequiredTenantId().toString());

        String ruleType = rule.getRuleType() == null ? "" : rule.getRuleType().toUpperCase(Locale.ROOT);
        String sql = switch (ruleType) {
            case "NOT_NULL" -> "SELECT count() AS violations FROM " + table
                    + " WHERE " + tenantFilter + " AND " + expression + " IS NULL";
            case "RANGE" -> rangeSql(table, tenantFilter, expression, rule.getConfig());
            case "UNIQUE" -> "SELECT count() AS violations FROM (SELECT " + expression
                    + " FROM " + table + " WHERE " + tenantFilter
                    + " GROUP BY " + expression + " HAVING count() > 1)";
            case "FRESHNESS" -> "SELECT count() AS violations FROM " + table
                    + " WHERE " + tenantFilter + " AND " + expression
                    + " < now() - INTERVAL " + freshnessHours(rule.getConfig()) + " HOUR";
            default -> throw new BusinessException("Unsupported quality rule type: " + rule.getRuleType());
        };

        List<Map<String, Object>> rows = clickHouseExecutor.query(sql, List.of("violations"),
                properties.getQueryTimeoutSeconds());
        long violations = extractViolations(rows);
        boolean passed = violations == 0;
        String message = passed
                ? "Check passed: 0 violating rows"
                : "Check failed: " + violations + " violating rows";
        log.debug("Quality rule {} ({}) on {}.{}: violations={}", rule.getRuleCode(),
                ruleType, rule.getDatasetCode(), rule.getTargetField(), violations);
        return new QualityCheckResult(passed, BigDecimal.valueOf(violations), message);
    }

    private String rangeSql(String table, String tenantFilter, String expression,
                            Map<String, Object> config) {
        List<String> conditions = new ArrayList<>();
        if (config != null) {
            if (config.get("min") != null) {
                conditions.add(expression + " < " + number(config.get("min")));
            }
            if (config.get("max") != null) {
                conditions.add(expression + " > " + number(config.get("max")));
            }
        }
        if (conditions.isEmpty()) {
            throw new BusinessException("RANGE rule requires config.min and/or config.max");
        }
        return "SELECT count() AS violations FROM " + table + " WHERE " + tenantFilter
                + " AND (" + String.join(" OR ", conditions) + ")";
    }

    private int freshnessHours(Map<String, Object> config) {
        Object value = config == null ? null : config.get("maxAgeHours");
        if (value == null) {
            return DEFAULT_FRESHNESS_HOURS;
        }
        try {
            int hours = new BigDecimal(value.toString()).intValueExact();
            if (hours < 1 || hours > 24 * 365) {
                throw new BusinessException("maxAgeHours must be between 1 and 8760");
            }
            return hours;
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new BusinessException("Invalid maxAgeHours: " + value);
        }
    }

    private String number(Object value) {
        try {
            return new BigDecimal(value.toString()).toPlainString();
        } catch (NumberFormatException ex) {
            throw new BusinessException("Invalid numeric rule config: " + value);
        }
    }

    private long extractViolations(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        Object value = rows.get(0).get("violations");
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            throw new BusinessException("Unexpected quality check result: " + value);
        }
    }

    public record QualityCheckResult(boolean passed, BigDecimal metricValue, String message) {
    }
}
