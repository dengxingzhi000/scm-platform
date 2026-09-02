package com.scmcloud.analytics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.analytics.api.dto.MetricDTO;
import com.scmcloud.analytics.domain.entity.MetricDefinition;
import com.scmcloud.analytics.mapper.MetricMapper;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricService {

    private static final Set<String> VALID_METRIC_TYPES = Set.of("BASE", "DERIVED", "COMPOSITE");
    private static final Set<String> VALID_DATA_TYPES = Set.of(
        "DECIMAL", "BIGINT", "DOUBLE", "INT", "STRING"
    );
    private static final Set<String> VALID_AGG_FUNCS = Set.of(
        "SUM", "AVG", "COUNT", "MAX", "MIN", "COUNT_DISTINCT", "NONE"
    );

    private final MetricMapper metricMapper;

    @Master(reason = "Create metric definition")
    @Transactional(rollbackFor = Exception.class)
    public MetricDefinition create(MetricDTO dto) {
        validate(dto);
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        MetricDefinition entity = MetricDefinition.builder()
                .tenantId(tenantId)
                .metricCode(dto.getMetricCode())
                .metricName(dto.getMetricName())
                .description(dto.getDescription())
                .metricType(dto.getMetricType())
                .dataType(dto.getDataType())
                .aggFunc(dto.getAggFunc())
                .expr(dto.getExpr())
                .unit(dto.getUnit())
                .format(dto.getFormat())
                .visible(dto.getVisible() != null ? dto.getVisible() : Boolean.TRUE)
                .status(dto.getStatus() != null ? dto.getStatus() : 1)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .createTime(now)
                .updateTime(now)
                .deleted(false)
                .remark(dto.getRemark())
                .build();

        try {
            metricMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("Metric code already exists: " + dto.getMetricCode());
        }
        log.info("Created metric: code={}, type={}, tenant={}",
                entity.getMetricCode(), entity.getMetricType(), tenantId);
        return entity;
    }

    @Master(reason = "Update metric definition")
    @Transactional(rollbackFor = Exception.class)
    public MetricDefinition update(UUID id, MetricDTO dto) {
        validate(dto);
        MetricDefinition existing = metricMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Metric not found: " + id);
        }
        existing.setMetricCode(dto.getMetricCode());
        existing.setMetricName(dto.getMetricName());
        existing.setDescription(dto.getDescription());
        existing.setMetricType(dto.getMetricType());
        existing.setDataType(dto.getDataType());
        existing.setAggFunc(dto.getAggFunc());
        existing.setExpr(dto.getExpr());
        existing.setUnit(dto.getUnit());
        existing.setFormat(dto.getFormat());
        if (dto.getVisible() != null) existing.setVisible(dto.getVisible());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        if (dto.getSortOrder() != null) existing.setSortOrder(dto.getSortOrder());
        if (dto.getRemark() != null) existing.setRemark(dto.getRemark());
        existing.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));

        try {
            metricMapper.updateById(existing);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("Metric code already exists: " + dto.getMetricCode());
        }
        return existing;
    }

    @Master(reason = "Soft-delete metric definition")
    @Transactional(rollbackFor = Exception.class)
    public void delete(UUID id) {
        int rows = metricMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException("Metric not found: " + id);
        }
        log.info("Soft-deleted metric: id={}", id);
    }

    @Slave( "Read metric by id")
    public MetricDefinition getById(UUID id) {
        return metricMapper.selectById(id);
    }

    @Slave( "Read metric by tenant-scoped code")
    public MetricDefinition getByCode(String code) {
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        return metricMapper.selectByTenantAndCode(tenantId, code);
    }

    @Slave( "Paged metric list scoped to current tenant")
    public Page<MetricDefinition> list(int page, int pageSize, String keyword) {
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        LambdaQueryWrapper<MetricDefinition> wrapper = Wrappers.lambdaQuery(MetricDefinition.class)
                .eq(MetricDefinition::getTenantId, tenantId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(MetricDefinition::getMetricCode, keyword)
                    .or().like(MetricDefinition::getMetricName, keyword));
        }
        wrapper.orderByAsc(MetricDefinition::getSortOrder);
        wrapper.orderByAsc(MetricDefinition::getMetricCode);
        return metricMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    private void validate(MetricDTO dto) {
        if (dto == null) {
            throw new BusinessException("DTO must not be null");
        }
        if (dto.getMetricCode() == null || dto.getMetricCode().isBlank()) {
            throw new BusinessException("metricCode is required");
        }
        if (dto.getMetricName() == null || dto.getMetricName().isBlank()) {
            throw new BusinessException("metricName is required");
        }
        if (dto.getMetricType() == null || !VALID_METRIC_TYPES.contains(dto.getMetricType())) {
            throw new BusinessException("metricType must be one of " + VALID_METRIC_TYPES);
        }
        if (dto.getDataType() == null || !VALID_DATA_TYPES.contains(dto.getDataType())) {
            throw new BusinessException("dataType must be one of " + VALID_DATA_TYPES);
        }
        if (dto.getAggFunc() != null && !VALID_AGG_FUNCS.contains(dto.getAggFunc())) {
            throw new BusinessException("aggFunc must be one of " + VALID_AGG_FUNCS);
        }
        if (("DERIVED".equals(dto.getMetricType()) || "COMPOSITE".equals(dto.getMetricType()))
                && (dto.getExpr() == null || dto.getExpr().isBlank())) {
            throw new BusinessException("expr is required for DERIVED and COMPOSITE metrics");
        }
    }
}