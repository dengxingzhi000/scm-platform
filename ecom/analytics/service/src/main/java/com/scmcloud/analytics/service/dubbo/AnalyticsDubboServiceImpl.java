package com.scmcloud.analytics.service.dubbo;

import com.scmcloud.analytics.api.AnalyticsDubboService;
import com.scmcloud.analytics.api.dto.DatasetQueryRequest;
import com.scmcloud.analytics.api.dto.DatasetQueryResult;
import com.scmcloud.analytics.query.QueryEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;
import java.util.UUID;

@Slf4j
@DubboService
@RequiredArgsConstructor
public class AnalyticsDubboServiceImpl implements AnalyticsDubboService {

    private final QueryEngine queryEngine;

    @Override
    public DatasetQueryResult query(DatasetQueryRequest request, UUID tenantId) {
        log.debug("[Dubbo] analytics query: dataset={}, tenant={}",
                request == null ? null : request.datasetCode(), tenantId);
        return queryEngine.query(request, tenantId);
    }

    @Override
    public DatasetQueryResult query(DatasetQueryRequest request, UUID tenantId, List<String> organizationIds) {
        log.debug("[Dubbo] analytics query: dataset={}, tenant={}, organizations={}",
                request == null ? null : request.datasetCode(), tenantId,
                organizationIds == null ? 0 : organizationIds.size());
        return queryEngine.query(request, tenantId, organizationIds);
    }
}
