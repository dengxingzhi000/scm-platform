package com.scmcloud.analytics.api.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Result of a {@link DatasetQueryRequest}.
 *
 * <p>{@code columns} carries the requested field codes in select order;
 * {@code rows} uses the same codes as map keys so the shape is stable even
 * when a query returns zero rows.</p>
 */
public record DatasetQueryResult(
        String datasetCode,
        List<Column> columns,
        List<Map<String, Object>> rows,
        int rowCount,
        int limit,
        long elapsedMs) implements Serializable {

    public record Column(String code, String dataType) implements Serializable {
    }
}
