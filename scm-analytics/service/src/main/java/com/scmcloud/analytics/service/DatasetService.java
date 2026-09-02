package com.scmcloud.analytics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.analytics.api.dto.DatasetDTO;
import com.scmcloud.analytics.domain.entity.DatasetDefinition;
import com.scmcloud.analytics.mapper.DatasetMapper;
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
public class DatasetService {

    private static final Set<String> VALID_DATASET_TYPES = Set.of(
        "WIDE_TABLE", "VIEW", "API", "CUSTOM"
    );
    private static final Set<String> VALID_SOURCE_TYPES = Set.of(
        "CLICKHOUSE", "POSTGRES", "MYSQL", "API"
    );

    private final DatasetMapper datasetMapper;

    @Master(reason = "Create dataset definition")
    @Transactional(rollbackFor = Exception.class)
    public DatasetDefinition create(DatasetDTO dto) {
        validate(dto);
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DatasetDefinition entity = DatasetDefinition.builder()
                .tenantId(tenantId)
                .datasetCode(dto.getDatasetCode())
                .datasetName(dto.getDatasetName())
                .description(dto.getDescription())
                .datasetType(dto.getDatasetType())
                .sourceType(dto.getSourceType())
                .sourceTable(dto.getSourceTable())
                .sourceConfig(dto.getSourceConfig())
                .status(dto.getStatus() != null ? dto.getStatus() : 1)
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : Boolean.TRUE)
                .createTime(now)
                .updateTime(now)
                .deleted(false)
                .remark(dto.getRemark())
                .build();

        try {
            datasetMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("Dataset code already exists: " + dto.getDatasetCode());
        }
        log.info("Created dataset: code={}, type={}, source={}, tenant={}",
                entity.getDatasetCode(), entity.getDatasetType(),
                entity.getSourceType(), tenantId);
        return entity;
    }

    @Master(reason = "Update dataset definition")
    @Transactional(rollbackFor = Exception.class)
    public DatasetDefinition update(UUID id, DatasetDTO dto) {
        validate(dto);
        DatasetDefinition existing = datasetMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Dataset not found: " + id);
        }
        existing.setDatasetCode(dto.getDatasetCode());
        existing.setDatasetName(dto.getDatasetName());
        existing.setDescription(dto.getDescription());
        existing.setDatasetType(dto.getDatasetType());
        existing.setSourceType(dto.getSourceType());
        if (dto.getSourceTable() != null) existing.setSourceTable(dto.getSourceTable());
        if (dto.getSourceConfig() != null) existing.setSourceConfig(dto.getSourceConfig());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        if (dto.getEnabled() != null) existing.setEnabled(dto.getEnabled());
        if (dto.getRemark() != null) existing.setRemark(dto.getRemark());
        existing.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));

        try {
            datasetMapper.updateById(existing);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("Dataset code already exists: " + dto.getDatasetCode());
        }
        return existing;
    }

    @Master(reason = "Soft-delete dataset definition")
    @Transactional(rollbackFor = Exception.class)
    public void delete(UUID id) {
        int rows = datasetMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException("Dataset not found: " + id);
        }
        log.info("Soft-deleted dataset: id={}", id);
    }

    @Slave( "Read dataset by id")
    public DatasetDefinition getById(UUID id) {
        return datasetMapper.selectById(id);
    }

    @Slave( "Read dataset by tenant-scoped code")
    public DatasetDefinition getByCode(String code) {
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        return datasetMapper.selectByTenantAndCode(tenantId, code);
    }

    @Slave( "Paged dataset list scoped to current tenant")
    public Page<DatasetDefinition> list(int page, int pageSize, String keyword) {
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        LambdaQueryWrapper<DatasetDefinition> wrapper = Wrappers.lambdaQuery(DatasetDefinition.class)
                .eq(DatasetDefinition::getTenantId, tenantId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(DatasetDefinition::getDatasetCode, keyword)
                    .or().like(DatasetDefinition::getDatasetName, keyword));
        }
        wrapper.orderByAsc(DatasetDefinition::getDatasetCode);
        return datasetMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    private void validate(DatasetDTO dto) {
        if (dto == null) {
            throw new BusinessException("DTO must not be null");
        }
        if (dto.getDatasetCode() == null || dto.getDatasetCode().isBlank()) {
            throw new BusinessException("datasetCode is required");
        }
        if (dto.getDatasetName() == null || dto.getDatasetName().isBlank()) {
            throw new BusinessException("datasetName is required");
        }
        if (dto.getDatasetType() == null || !VALID_DATASET_TYPES.contains(dto.getDatasetType())) {
            throw new BusinessException("datasetType must be one of " + VALID_DATASET_TYPES);
        }
        if (dto.getSourceType() == null || !VALID_SOURCE_TYPES.contains(dto.getSourceType())) {
            throw new BusinessException("sourceType must be one of " + VALID_SOURCE_TYPES);
        }
    }
}