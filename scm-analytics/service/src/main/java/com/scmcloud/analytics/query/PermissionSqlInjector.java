package com.scmcloud.analytics.query;

import com.scmcloud.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the mandatory row-level predicates for every generated query.
 */
@Component
public class PermissionSqlInjector {

    public String tenantPredicate(UUID tenantId) {
        if (tenantId == null) {
            throw new BusinessException("Tenant id is required for analytics queries");
        }
        return "tenant_id = " + SqlSafety.quoteString(tenantId.toString());
    }

    public String organizationPredicate(List<String> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return null;
        }
        String values = organizationIds.stream()
                .map(SqlSafety::organizationId)
                .collect(Collectors.joining(", "));
        return "organization_id IN (" + values + ")";
    }
}
