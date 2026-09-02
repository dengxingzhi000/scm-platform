package com.scmcloud.analytics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.analytics.api.dto.DatasetFieldDTO;
import com.scmcloud.analytics.domain.entity.DatasetFieldDefinition;
import com.scmcloud.analytics.mapper.DatasetFieldMapper;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetFieldService {

    private static final Set<String> VALID_FIELD_TYPES = Set.of(
        "METRIC", "DIMENSION", "CALCULATED"
    );
    private static final Set<String> VALID_DATA_TYPES = Set.of(
        "VARCHAR", "INT", "BIGINT", "DECIMAL", "DOUBLE", "DATE", "TIMESTAMPTZ", "BOOLEAN"
    );

    private final DatasetFieldMapper datasetFieldMapper;

    @Master(reason = "Create dataset field")
    @Transactional(rollbackFor = Exception.class)
    public DatasetFieldDefinition create(DatasetFieldDTO dto) {
        validate(dto);
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DatasetFieldDefinition entity = DatasetFieldDefinition.builder()
                .tenantId(tenantId)
                .datasetId(dto.getDatasetId())
                .fieldCode(dto.getFieldCode())
                .fieldName(dto.getFieldName())
                .description(dto.getDescription())
                .fieldType(dto.getFieldType())
                .dataType(dto.getDataType())
                .metricId(dto.getMetricId())
                .dimensionId(dto.getDimensionId())
                .expr(dto.getExpr())
                .isPartitionKey(dto.getIsPartitionKey() != null ? dto.getIsPartitionKey() : Boolean.FALSE)
                .isHidden(dto.getIsHidden() != null ? dto.getIsHidden() : Boolean.FALSE)
                .isRequired(dto.getIsRequired() != null ? dto.getIsRequired() : Boolean.FALSE)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .defaultValue(dto.getDefaultValue())
                .status(dto.getStatus() != null ? dto.getStatus() : 1)
                .createTime(now)
                .updateTime(now)
                .deleted(false)
                .remark(dto.getRemark())
                .build();

        try {
            datasetFieldMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("Field code already exists for this dataset: " + dto.getFieldCode());
        }
        log.info("Created dataset field: dataset={}, code={}, type={}, tenant={}",
                entity.getDatasetId(), entity.getFieldCode(),
                entity.getFieldType(), tenantId);
        return entity;
    }

    @Master(reason = "Update dataset field")
    @Transactional(rollbackFor = Exception.class)
    public DatasetFieldDefinition update(UUID id, DatasetFieldDTO dto) {
        validate(dto);
        DatasetFieldDefinition existing = datasetFieldMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Dataset field not found: " + id);
        }
        existing.setDatasetId(dto.getDatasetId());
        existing.setFieldCode(dto.getFieldCode());
        existing.setFieldName(dto.getFieldName());
        existing.setDescription(dto.getDescription());
        existing.setFieldType(dto.getFieldType());
        existing.setDataType(dto.getDataType());
        if (dto.getMetricId() != null) existing.setMetricId(dto.getMetricId());
        if (dto.getDimensionId() != null) existing.setDimensionId(dto.getDimensionId());
        if (dto.getExpr() != null) existing.setExpr(dto.getExpr());
        if (dto.getIsPartitionKey() != null) existing.setIsPartitionKey(dto.getIsPartitionKey());
        if (dto.getIsHidden() != null) existing.setIsHidden(dto.getIsHidden());
        if (dto.getIsRequired() != null) existing.setIsRequired(dto.getIsRequired());
        if (dto.getSortOrder() != null) existing.setSortOrder(dto.getSortOrder());
        if (dto.getDefaultValue() != null) existing.setDefaultValue(dto.getDefaultValue());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        if (dto.getRemark() != null) existing.setRemark(dto.getRemark());
        existing.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));

        try {
            datasetFieldMapper.updateById(existing);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("Field code already exists for this dataset: " + dto.getFieldCode());
        }
        return existing;
    }

    @Master(reason = "Soft-delete dataset field")
    @Transactional(rollbackFor = Exception.class)
    public void delete(UUID id) {
        int rows = datasetFieldMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException("Dataset field not found: " + id);
        }
        log.info("Soft-deleted dataset field: id={}", id);
    }

    @Slave( "Read dataset field by id")
    public DatasetFieldDefinition getById(UUID id) {
        return datasetFieldMapper.selectById(id);
    }

    @Slave( "List dataset fields by dataset, tenant-scoped")
    public List<DatasetFieldDefinition> listByDataset(UUID datasetId) {
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        return datasetFieldMapper.selectByDataset(tenantId, datasetId);
    }

    @Slave( "Paged dataset field list scoped to current tenant")
    public Page<DatasetFieldDefinition> list(int page, int pageSize, String keyword) {
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        LambdaQueryWrapper<DatasetFieldDefinition> wrapper = Wrappers.lambdaQuery(DatasetFieldDefinition.class)
                .eq(DatasetFieldDefinition::getTenantId, tenantId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(DatasetFieldDefinition::getFieldCode, keyword)
                    .or().like(DatasetFieldDefinition::getFieldName, keyword));
        }
        wrapper.orderByAsc(DatasetFieldDefinition::getSortOrder);
        wrapper.orderByAsc(DatasetFieldDefinition::getFieldCode);
        return datasetFieldMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    private void validate(DatasetFieldDTO dto) {
        if (dto == null) {
            throw new BusinessException("DTO must not be null");
        }
        if (dto.getDatasetId() == null) {
            throw new BusinessException("datasetId is required");
        }
        if (dto.getFieldCode() == null || dto.getFieldCode().isBlank()) {
            throw new BusinessException("fieldCode is required");
        }
        if (dto.getFieldName() == null || dto.getFieldName().isBlank()) {
            throw new BusinessException("fieldName is required");
        }
        if (dto.getFieldType() == null || !VALID_FIELD_TYPES.contains(dto.getFieldType())) {
            throw new BusinessException("fieldType must be one of " + VALID_FIELD_TYPES);
        }
        if (dto.getDataType() == null || !VALID_DATA_TYPES.contains(dto.getDataType())) {
            throw new BusinessException("dataType must be one of " + VALID_DATA_TYPES);
        }
        if ("METRIC".equals(dto.getFieldType()) && dto.getMetricId() == null) {
            throw new BusinessException("metricId is required when fieldType=METRIC");
        }
        if ("DIMENSION".equals(dto.getFieldType()) && dto.getDimensionId() == null) {
            throw new BusinessException("dimensionId is required when fieldType=DIMENSION");
        }
    }
}