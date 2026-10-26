package com.scmcloud.analytics.controller;

import com.scmcloud.analytics.api.dto.DatasetQueryRequest;
import com.scmcloud.analytics.api.dto.DatasetQueryResult;
import com.scmcloud.analytics.query.QueryEngine;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.tenant.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Declarative analytics query API for the BI dashboard and external consumers.
 *
 * <p>The organization scope is read from the authenticated header injected by
 * the gateway; it is intentionally not part of the request body so a caller
 * cannot widen its own data scope.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "分析查询", description = "面向 BI 看板与外部调用方的声明式分析查询接口。")
public class AnalyticsQueryController {

    private final QueryEngine queryEngine;

    @Operation(summary = "执行分析查询", description = "按数据集与指标、维度定义执行声明式分析查询，并按组织范围过滤数据。")
    @PostMapping("/query")
    public ApiResponse<DatasetQueryResult> query(
            @RequestBody DatasetQueryRequest request,
            @RequestHeader(value = "X-Organization-Ids", required = false) String organizationIds) {
        List<String> organizations = parseOrganizationIds(organizationIds);
        if (organizations.isEmpty()) {
            return ApiResponse.success(queryEngine.query(request));
        }
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        log.info("[API] analytics query: dataset={}, organizations={}",
                request.datasetCode(), organizations.size());
        return ApiResponse.success(queryEngine.query(request, tenantId, organizations));
    }

    private List<String> parseOrganizationIds(String header) {
        if (header == null || header.isBlank()) {
            return List.of();
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
