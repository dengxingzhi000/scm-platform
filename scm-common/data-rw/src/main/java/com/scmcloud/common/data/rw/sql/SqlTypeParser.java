package com.scmcloud.common.data.rw.sql;

import java.util.Set;
import java.util.regex.Pattern;

public final class SqlTypeParser {

    private static final Set<String> WRITE_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "REPLACE",
            "CREATE", "ALTER", "DROP", "TRUNCATE",
            "GRANT", "REVOKE", "LOCK", "UNLOCK",
            "CALL", "MERGE", "UPSERT"
    );

    private static final Set<String> READ_KEYWORDS = Set.of(
            "SELECT", "SHOW", "DESCRIBE", "EXPLAIN"
    );

    private static final Pattern FOR_UPDATE_PATTERN = Pattern.compile(
            ".*\\bFOR\\s+UPDATE\\b.*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern LOCK_IN_SHARE_MODE_PATTERN = Pattern.compile(
            ".*\\bLOCK\\s+IN\\s+SHARE\\s+MODE\\b.*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern MASTER_HINT = Pattern.compile(
            "/\\*\\s*MASTER\\s*\\*/",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SLAVE_HINT = Pattern.compile(
            "/\\*\\s*SLAVE\\s*\\*/",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SLAVE_NAME_HINT = Pattern.compile(
            "/\\*\\s*SLAVE\\s*\\(\\s*(\\w+)\\s*\\)\\s*\\*/",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "/\\*.*?\\*/|--.*$",
            Pattern.DOTALL | Pattern.MULTILINE
    );

    private SqlTypeParser() {
    }

    public enum SqlType {
        READ,
        WRITE,
        UNKNOWN
    }

    public record RoutingHint(HintType type, String slaveName) {
        public enum HintType {
            NONE,
            MASTER,
            SLAVE
        }
    }

    public static SqlType parse(String sql) {
        if (sql == null || sql.isBlank()) {
            return SqlType.UNKNOWN;
        }

        String trimmedSql = sql.trim();

        if (FOR_UPDATE_PATTERN.matcher(trimmedSql).matches()) {
            return SqlType.WRITE;
        }

        if (LOCK_IN_SHARE_MODE_PATTERN.matcher(trimmedSql).matches()) {
            return SqlType.WRITE;
        }

        String firstKeyword = extractFirstKeyword(trimmedSql);
        if (firstKeyword.isEmpty()) {
            return SqlType.UNKNOWN;
        }

        if (WRITE_KEYWORDS.contains(firstKeyword)) {
            return SqlType.WRITE;
        }

        if (READ_KEYWORDS.contains(firstKeyword)) {
            return SqlType.READ;
        }

        return SqlType.UNKNOWN;
    }

    public static RoutingHint parseHint(String sql) {
        if (sql == null || sql.isBlank()) {
            return new RoutingHint(RoutingHint.HintType.NONE, null);
        }

        if (MASTER_HINT.matcher(sql).find()) {
            return new RoutingHint(RoutingHint.HintType.MASTER, null);
        }

        var nameMatcher = SLAVE_NAME_HINT.matcher(sql);
        if (nameMatcher.find()) {
            return new RoutingHint(RoutingHint.HintType.SLAVE, nameMatcher.group(1));
        }

        if (SLAVE_HINT.matcher(sql).find()) {
            return new RoutingHint(RoutingHint.HintType.SLAVE, null);
        }

        return new RoutingHint(RoutingHint.HintType.NONE, null);
    }

    public static String removeHint(String sql) {
        if (sql == null) {
            return null;
        }
        return sql
                .replaceAll("/\\*\\s*MASTER\\s*\\*/", "")
                .replaceAll("/\\*\\s*SLAVE\\s*(\\(\\s*\\w+\\s*\\))?\\s*\\*/", "")
                .trim();
    }

    public static boolean isTransactionStatement(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }
        String keyword = extractFirstKeyword(sql.trim());
        return "BEGIN".equals(keyword)
                || "COMMIT".equals(keyword)
                || "ROLLBACK".equals(keyword)
                || "SAVEPOINT".equals(keyword)
                || "SET".equals(keyword);
    }

    private static String extractFirstKeyword(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }

        String cleaned = COMMENT_PATTERN.matcher(sql).replaceAll("").trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        int len = cleaned.length();
        for (int i = 0; i < len; i++) {
            char c = cleaned.charAt(i);
            if (c == ' ' || c == '\n' || c == '\t' || c == '\r' || c == '(') {
                return cleaned.substring(0, i).toUpperCase();
            }
        }

        return cleaned.toUpperCase();
    }
}
