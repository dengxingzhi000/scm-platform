package com.scmcloud.analytics.api;

import com.scmcloud.analytics.api.dto.DatasetQueryRequest;
import com.scmcloud.analytics.api.dto.DatasetQueryResult;

import java.util.List;
import java.util.UUID;

/**
 * Dubbo entry point for BI / Python / AI consumers.
 *
 * <p>Queries are declarative and tenant-scoped; implementations must never
 * accept raw SQL from callers.</p>
 */
public interface AnalyticsDubboService {

    /**
     * Execute a dataset query in the given tenant's context.
     *
     * @param request dataset query (never raw SQL)
     * @param tenantId tenant whose data should be queried
     * @return query result with columns and rows
     */
    DatasetQueryResult query(DatasetQueryRequest request, UUID tenantId);

    /**
     * Execute a dataset query with an optional organization scope.
     *
     * @param request dataset query
     * @param tenantId tenant whose data should be queried
     * @param organizationIds organization ids to restrict to; empty means no
     *                        organization filter
     */
    DatasetQueryResult query(DatasetQueryRequest request, UUID tenantId, List<String> organizationIds);
}
