package com.scmcloud.analytics.query;

import java.util.List;

/**
 * SQL produced by {@link SqlGenerator} together with the projected column
 * metadata. Only the generator may construct this type.
 */
public record GeneratedQuery(
        String sql,
        List<String> columns,
        List<String> dataTypes,
        int limit) {
}
