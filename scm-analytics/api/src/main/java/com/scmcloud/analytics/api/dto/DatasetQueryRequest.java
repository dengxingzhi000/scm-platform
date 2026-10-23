package com.scmcloud.analytics.api.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Declarative dataset query. Callers never pass raw SQL: every identifier is
 * resolved against the tenant's dataset metadata and rejected when unknown.
 *
 * <p>{@code dimensions} and {@code metrics} are {@code analytics_dataset_field}
 * field codes. {@code filters} and {@code orderBy} may only reference fields
 * that are present in the dataset.</p>
 */
public record DatasetQueryRequest(
        String datasetCode,
        List<String> dimensions,
        List<String> metrics,
        List<Filter> filters,
        List<OrderBy> orderBy,
        Integer limit) implements Serializable {

    public DatasetQueryRequest {
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
        filters = filters == null ? List.of() : List.copyOf(filters);
        orderBy = orderBy == null ? List.of() : List.copyOf(orderBy);
    }

    public record Filter(String field, String operator, List<String> values) implements Serializable {
        public Filter {
            values = values == null ? List.of() : List.copyOf(values);
        }
    }

    public record OrderBy(String field, String direction) implements Serializable {
    }
}
