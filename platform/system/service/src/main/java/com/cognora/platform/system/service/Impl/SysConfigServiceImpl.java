package com.cognora.platform.system.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cognora.platform.common.data.rw.annotation.Master;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.platform.common.dto.config.SysConfigItemDTO;
import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.util.UUIDv7Util;
import com.cognora.platform.system.domain.entity.SysConfig;
import com.cognora.platform.system.mapper.SysConfigMapper;
import com.cognora.platform.system.service.ISysConfigService;
import com.cognora.platform.system.service.support.PlatformAdminGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 配置服务：平台级默认为主，租户覆盖优先。
 *
 * <p>{@code sys_config} 已加入 {@code cognora.tenant.exclude-tables}，因此
 * {@code TenantInterceptor} 不会自动追加 {@code tenant_id} 条件——平台级行
 * （{@code tenant_id IS NULL}）必须在这里显式查询，租户过滤也由本服务负责。</p>
 *
 * @author Deng
 * @since 2026-11-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements ISysConfigService {

    private static final String SCOPE_PLATFORM = "PLATFORM";
    private static final String SCOPE_TENANT = "TENANT";
    private static final String DEFAULT_VALUE_TYPE = "STRING";

    private final SysConfigMapper configMapper;
    private final PlatformAdminGuard platformAdminGuard;

    @Override
    @Slave
    public List<SysConfigItemDTO> getMergedConfig() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        Map<String, SysConfigItemDTO> merged = new LinkedHashMap<>();
        for (SysConfig row : selectByTenant(null)) {
            merged.put(row.getConfigKey(), toDTO(row, SCOPE_PLATFORM));
        }
        for (SysConfig row : selectByTenant(tenantId)) {
            // 同键覆盖：保留平台级项的原位置，值替换为租户覆盖值，来源标记为 TENANT。
            merged.put(row.getConfigKey(), toDTO(row, SCOPE_TENANT));
        }
        return new ArrayList<>(merged.values());
    }

    @Override
    @Master
    @Transactional(rollbackFor = Exception.class)
    public SysConfigItemDTO updateConfig(SysConfigItemDTO request) {
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "配置项不能为空");
        }
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        String scope = resolveScope(request);
        if (SCOPE_PLATFORM.equals(scope) && !platformAdminGuard.isPlatformAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(),
                    "仅平台超级管理员可以修改平台级配置");
        }

        UUID rowTenantId = SCOPE_PLATFORM.equals(scope) ? null : tenantId;
        SysConfig saved = upsert(rowTenantId, request);
        log.info("系统配置更新完成: tenantId={} scope={} key={}",
                tenantId, scope, request.getConfigKey());
        return toDTO(saved, scope);
    }

    private List<SysConfig> selectByTenant(UUID tenantId) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<SysConfig>()
                .orderByAsc(SysConfig::getConfigKey);
        if (tenantId == null) {
            wrapper.isNull(SysConfig::getTenantId);
        } else {
            wrapper.eq(SysConfig::getTenantId, tenantId);
        }
        return configMapper.selectList(wrapper);
    }

    private SysConfig upsert(UUID tenantId, SysConfigItemDTO item) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, item.getConfigKey());
        if (tenantId == null) {
            wrapper.isNull(SysConfig::getTenantId);
        } else {
            wrapper.eq(SysConfig::getTenantId, tenantId);
        }

        String valueType = StringUtils.hasText(item.getValueType())
                ? item.getValueType() : DEFAULT_VALUE_TYPE;
        SysConfig existing = configMapper.selectOne(wrapper);
        if (existing == null) {
            SysConfig row = new SysConfig()
                    .setId(UUIDv7Util.generate())
                    .setTenantId(tenantId)
                    .setConfigKey(item.getConfigKey())
                    .setConfigValue(item.getConfigValue())
                    .setValueType(valueType)
                    .setDescription(item.getDescription())
                    .setDeleted(false);
            try {
                configMapper.insert(row);
                return configMapper.selectOne(wrapper);
            } catch (DuplicateKeyException concurrentInsert) {
                // 并发首次写入：唯一索引保证只有一条，另一条请求落败后改为更新。
                existing = configMapper.selectOne(wrapper);
                if (existing == null) {
                    throw concurrentInsert;
                }
            }
        }
        existing.setConfigValue(item.getConfigValue());
        existing.setValueType(valueType);
        if (item.getDescription() != null) {
            existing.setDescription(item.getDescription());
        }
        configMapper.updateById(existing);
        return configMapper.selectOne(wrapper);
    }

    private String resolveScope(SysConfigItemDTO item) {
        return StringUtils.hasText(item.getScope())
                ? item.getScope().trim().toUpperCase() : SCOPE_TENANT;
    }

    private SysConfigItemDTO toDTO(SysConfig row, String scope) {
        return new SysConfigItemDTO(
                row.getConfigKey(),
                row.getConfigValue(),
                row.getValueType(),
                row.getDescription(),
                scope,
                row.getTenantId(),
                row.getUpdateTime());
    }
}
