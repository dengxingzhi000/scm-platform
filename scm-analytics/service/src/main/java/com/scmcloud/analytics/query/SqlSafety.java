package com.scmcloud.analytics.query;

import com.scmcloud.common.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validation helpers for the declarative query engine.
 *
 * <p>The engine never forwards caller SQL. Identifiers come from tenant
 * metadata, but values still need type-aware serialization so that a malicious
 * string cannot break out of a literal.</p>
 */
public final class SqlSafety {

    static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern TABLE = Pattern.compile(
            "[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?");
    private static final Pattern EXPRESSION = Pattern.compile(
            "[A-Za-z0-9_ .,()\\]+\\-*/%<>=!:'\"|&]+");
    private static final Pattern ORG_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final Pattern DATE_TIME = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}([ T]\\d{2}:\\d{2}(:\\d{2})?)?(Z|[+-]\\d{2}:\\d{2})?");

    static final Set<String> AGG_FUNCTIONS = Set.of(
            "SUM", "AVG", "COUNT", "MAX", "MIN", "COUNT_DISTINCT", "NONE");

    private SqlSafety() {
    }

    public static String requireIdentifier(String value, String label) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw new BusinessException("Invalid " + label + ": " + value);
        }
        return value;
    }

    public static String requireTable(String value) {
        if (value == null || !TABLE.matcher(value).matches()) {
            throw new BusinessException("Invalid dataset source table: " + value);
        }
        return value;
    }

    public static String requireExpression(String expression, String label) {
        if (expression == null || expression.isBlank()) {
            throw new BusinessException(label + " expression must not be empty");
        }
        String trimmed = expression.trim();
        if (trimmed.contains(";") || trimmed.contains("--") || trimmed.contains("/*")
                || trimmed.contains("*/") || trimmed.contains("#")
                || trimmed.indexOf('\n') >= 0 || trimmed.indexOf('\r') >= 0
                || trimmed.indexOf('\0') >= 0) {
            throw new BusinessException("Unsafe characters in " + label + " expression");
        }
        if (!EXPRESSION.matcher(trimmed).matches()) {
            throw new BusinessException("Unsupported characters in " + label + " expression");
        }
        return trimmed;
    }

    public static String quoteString(String value) {
        if (value == null) {
            throw new BusinessException("SQL value must not be null");
        }
        if (value.indexOf('\0') >= 0) {
            throw new BusinessException("SQL value contains a NUL character");
        }
        return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'";
    }

    public static String quoteIdentifier(String identifier) {
        requireIdentifier(identifier, "identifier");
        return "`" + identifier + "`";
    }

    static String organizationId(String value) {
        if (value == null || !ORG_ID.matcher(value).matches()) {
            throw new BusinessException("Invalid organization id: " + value);
        }
        return quoteString(value);
    }

    static String literal(String dataType, String raw) {
        if (raw == null) {
            throw new BusinessException("Filter value must not be null");
        }
        String type = dataType == null ? "VARCHAR" : dataType.toUpperCase();
        try {
            return switch (type) {
                case "INT", "BIGINT" -> Long.toString(Long.parseLong(raw.trim()));
                case "DECIMAL", "DOUBLE" -> new BigDecimal(raw.trim()).toPlainString();
                case "BOOLEAN" -> {
                    String v = raw.trim().toLowerCase();
                    if ("true".equals(v) || "1".equals(v)) yield "true";
                    if ("false".equals(v) || "0".equals(v)) yield "false";
                    throw new BusinessException("Invalid boolean filter value: " + raw);
                }
                case "DATE" -> {
                    LocalDate date = LocalDate.parse(raw.trim());
                    yield "toDate(" + quoteString(date.toString()) + ")";
                }
                case "TIMESTAMPTZ" -> dateTimeLiteral(raw.trim());
                default -> quoteString(raw);
            };
        } catch (NumberFormatException | DateTimeParseException ex) {
            throw new BusinessException("Invalid " + type + " filter value: " + raw);
        }
    }

    private static String dateTimeLiteral(String raw) {
        if (!DATE_TIME.matcher(raw).matches()) {
            throw new BusinessException("Invalid TIMESTAMPTZ filter value: " + raw);
        }
        String normalized = raw.replace(' ', 'T');
        if (normalized.length() == 10) {
            return "toDate(" + quoteString(LocalDate.parse(normalized).toString()) + ")";
        }
        if (normalized.endsWith("Z") || normalized.contains("+")
                || normalized.lastIndexOf('-') > 10) {
            return "parseDateTimeBestEffort(" + quoteString(OffsetDateTime.parse(normalized).toString()) + ")";
        }
        return "parseDateTimeBestEffort(" + quoteString(LocalDateTime.parse(normalized).toString()) + ")";
    }
}
