package com.scmcloud.analytics.query;

import com.scmcloud.analytics.api.dto.DatasetQueryRequest;
import com.scmcloud.analytics.api.dto.DatasetQueryResult;
import com.scmcloud.analytics.config.ClickHouseProperties;
import com.scmcloud.analytics.domain.entity.DatasetDefinition;
import com.scmcloud.analytics.domain.entity.DatasetFieldDefinition;
import com.scmcloud.analytics.domain.entity.MetricDefinition;
import com.scmcloud.analytics.mapper.MetricMapper;
import com.scmcloud.analytics.service.DatasetFieldService;
import com.scmcloud.analytics.service.DatasetService;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates metadata lookup, SQL generation and ClickHouse execution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryEngine {

    private final DatasetService datasetService;
    private final DatasetFieldService datasetFieldService;
    private final MetricMapper metricMapper;
    private final SqlGenerator sqlGenerator;
    private final ClickHouseExecutor clickHouseExecutor;
    private final ClickHouseProperties properties;

    @Slave("Read analytics metadata and serve query")
    public DatasetQueryResult query(DatasetQueryRequest request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return queryInContext(request, new QueryContext(tenantId, List.of()));
    }

    @Slave("Read analytics metadata and serve tenant query")
    public DatasetQueryResult query(DatasetQueryRequest request, UUID tenantId) {
        return query(request, tenantId, List.of());
    }

    public DatasetQueryResult query(DatasetQueryRequest request, UUID tenantId, List<String> organizationIds) {
        if (tenantId == null) {
            throw new BusinessException("Tenant id is required for analytics queries");
        }
        return TenantContextHolder.executeInTenantContext(tenantId,
                () -> queryInContext(request, new QueryContext(tenantId, organizationIds)));
    }

    private DatasetQueryResult queryInContext(DatasetQueryRequest request, QueryContext context) {
        long startedAt = System.nanoTime();
        if (request == null) {
            throw new BusinessException("Query request must not be null");
        }
        if (request.datasetCode() == null || request.datasetCode().isBlank()) {
            throw new BusinessException("datasetCode is required");
        }

        DatasetDefinition dataset = datasetService.getByCode(request.datasetCode());
        if (dataset == null) {
            throw new BusinessException("Dataset not found: " + request.datasetCode());
        }
        List<DatasetFieldDefinition> fields = datasetFieldService.listByDataset(dataset.getId());
        if (fields == null || fields.isEmpty()) {
            throw new BusinessException("Dataset has no queryable fields: " + request.datasetCode());
        }
        List<MetricDefinition> metrics = loadMetrics(fields, context.tenantId());

        GeneratedQuery generated = sqlGenerator.generate(dataset, fields, metrics, request, context);
        List<Map<String, Object>> rows = clickHouseExecutor.query(
                generated.sql(), generated.columns(), properties.getQueryTimeoutSeconds());

        List<DatasetQueryResult.Column> columns = new ArrayList<>(generated.columns().size());
        for (int i = 0; i < generated.columns().size(); i++) {
            String dataType = generated.dataTypes().get(i);
            columns.add(new DatasetQueryResult.Column(
                    generated.columns().get(i),
                    dataType == null ? "VARCHAR" : dataType));
        }

        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
        log.info("Analytics query: dataset={}, dimensions={}, metrics={}, rows={}, elapsedMs={}",
                dataset.getDatasetCode(), request.dimensions().size(), request.metrics().size(),
                rows.size(), elapsedMs);
        log.debug("Generated analytics SQL: {}", generated.sql());

        return new DatasetQueryResult(
                dataset.getDatasetCode(),
                List.copyOf(columns),
                rows,
                rows.size(),
                generated.limit(),
                elapsedMs);
    }

    private List<MetricDefinition> loadMetrics(List<DatasetFieldDefinition> fields, UUID tenantId) {
        Map<UUID, MetricDefinition> byId = new LinkedHashMap<>();
        for (DatasetFieldDefinition field : fields) {
            if (field.getMetricId() != null) {
                MetricDefinition metric = metricMapper.selectById(field.getMetricId());
                if (metric != null) {
                    byId.put(metric.getId(), metric);
                }
            }
        }
        List<MetricDefinition> visible = metricMapper.selectVisibleByTenant(tenantId.toString());
        if (visible != null) {
            for (MetricDefinition metric : visible) {
                byId.putIfAbsent(metric.getId(), metric);
            }
        }
        return List.copyOf(byId.values());
    }
}
