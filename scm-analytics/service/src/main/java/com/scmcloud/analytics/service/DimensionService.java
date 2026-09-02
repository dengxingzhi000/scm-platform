package com.scmcloud.analytics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.analytics.api.dto.DimensionDTO;
import com.scmcloud.analytics.domain.entity.DimensionDefinition;
import com.scmcloud.analytics.mapper.DimensionMapper;
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
public class DimensionService {

    private static final Set<String> VALID_DIM_TYPES = Set.of(
        "TIME", "CATEGORICAL", "NUMERIC", "GEO", "HIERARCHICAL"
    );
    private static final Set<String> VALID_DATA_TYPES = Set.of(
        "VARCHAR", "INT", "BIGINT", "DATE", "TIMESTAMPTZ", "DECIMAL", "BOOLEAN"
    );
    private static final int MIN_HIERARCHY_LEVEL = 1;
    private static final int MAX_HIERARCHY_LEVEL = 5;

    private final DimensionMapper dimensionMapper;

    @Master(reason = "Create dimension definition")
    @Transactional(rollbackFor = Exception.class)
    public DimensionDefinition create(DimensionDTO dto) {
        validate(dto);
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DimensionDefinition entity = DimensionDefinition.builder()
                .tenantId(tenantId)
                .dimCode(dto.getDimCode())
                .dimName(dto.getDimName())
                .description(dto.getDescription())
                .dimType(dto.getDimType())
                .dataType(dto.getDataType())
                .parentDimId(dto.getParentDimId())
                .hierarchyLevel(dto.getHierarchyLevel() != null ? dto.getHierarchyLevel() : 1)
                .isRequired(dto.getIsRequired() != null ? dto.getIsRequired() : Boolean.FALSE)
                .dictCode(dto.getDictCode())
                .status(dto.getStatus() != null ? dto.getStatus() : 1)
                .createTime(now)
                .updateTime(now)
                .deleted(false)
                .remark(dto.getRemark())
                .build();

        try {
            dimensionMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("Dimension code already exists: " + dto.getDimCode());
        }
        log.info("Created dimension: code={}, type={}, tenant={}",
                entity.getDimCode(), entity.getDimType(), tenantId);
        return entity;
    }

    @Master(reason = "Update dimension definition")
    @Transactional(rollbackFor = Exception.class)
    public DimensionDefinition update(UUID id, DimensionDTO dto) {
        validate(dto);
        DimensionDefinition existing = dimensionMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Dimension not found: " + id);
        }
        existing.setDimCode(dto.getDimCode());
        existing.setDimName(dto.getDimName());
        existing.setDescription(dto.getDescription());
        existing.setDimType(dto.getDimType());
        existing.setDataType(dto.getDataType());
        existing.setParentDimId(dto.getParentDimId());
        if (dto.getHierarchyLevel() != null) existing.setHierarchyLevel(dto.getHierarchyLevel());
        if (dto.getIsRequired() != null) existing.setIsRequired(dto.getIsRequired());
        if (dto.getDictCode() != null) existing.setDictCode(dto.getDictCode());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        if (dto.getRemark() != null) existing.setRemark(dto.getRemark());
        existing.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));

        try {
            dimensionMapper.updateById(existing);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("Dimension code already exists: " + dto.getDimCode());
        }
        return existing;
    }

    @Master(reason = "Soft-delete dimension definition")
    @Transactional(rollbackFor = Exception.class)
    public void delete(UUID id) {
        int rows = dimensionMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException("Dimension not found: " + id);
        }
        log.info("Soft-deleted dimension: id={}", id);
    }

    @Slave( "Read dimension by id")
    public DimensionDefinition getById(UUID id) {
        return dimensionMapper.selectById(id);
    }

    @Slave( "Read dimension by tenant-scoped code")
    public DimensionDefinition getByCode(String code) {
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        return dimensionMapper.selectByTenantAndCode(tenantId, code);
    }

    @Slave( "Paged dimension list scoped to current tenant")
    public Page<DimensionDefinition> list(int page, int pageSize, String keyword) {
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        LambdaQueryWrapper<DimensionDefinition> wrapper = Wrappers.lambdaQuery(DimensionDefinition.class)
                .eq(DimensionDefinition::getTenantId, tenantId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(DimensionDefinition::getDimCode, keyword)
                    .or().like(DimensionDefinition::getDimName, keyword));
        }
        wrapper.orderByAsc(DimensionDefinition::getHierarchyLevel);
        wrapper.orderByAsc(DimensionDefinition::getDimCode);
        return dimensionMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    private void validate(DimensionDTO dto) {
        if (dto == null) {
            throw new BusinessException("DTO must not be null");
        }
        if (dto.getDimCode() == null || dto.getDimCode().isBlank()) {
            throw new BusinessException("dimCode is required");
        }
        if (dto.getDimName() == null || dto.getDimName().isBlank()) {
            throw new BusinessException("dimName is required");
        }
        if (dto.getDimType() == null || !VALID_DIM_TYPES.contains(dto.getDimType())) {
            throw new BusinessException("dimType must be one of " + VALID_DIM_TYPES);
        }
        if (dto.getDataType() == null || !VALID_DATA_TYPES.contains(dto.getDataType())) {
            throw new BusinessException("dataType must be one of " + VALID_DATA_TYPES);
        }
        if (dto.getHierarchyLevel() != null
                && (dto.getHierarchyLevel() < MIN_HIERARCHY_LEVEL
                    || dto.getHierarchyLevel() > MAX_HIERARCHY_LEVEL)) {
            throw new BusinessException("hierarchyLevel must be between " + MIN_HIERARCHY_LEVEL + " and " + MAX_HIERARCHY_LEVEL);
        }
    }
}