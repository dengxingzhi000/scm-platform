package com.scmcloud.analytics.query;

import java.util.List;
import java.util.UUID;

/**
 * Server-side security context for a dataset query. Organization ids are never
 * accepted from the request body — callers pass them from an authenticated
 * context (REST header resolved by the gateway, or Dubbo invocation).
 */
public record QueryContext(UUID tenantId, List<String> organizationIds) {

    public QueryContext {
        organizationIds = organizationIds == null ? List.of() : List.copyOf(organizationIds);
    }
}
