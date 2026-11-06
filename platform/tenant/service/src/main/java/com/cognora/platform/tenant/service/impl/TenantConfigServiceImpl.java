package com.cognora.platform.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.tenant.domain.entity.TenantConfig;
import com.cognora.platform.tenant.mapper.TenantConfigMapper;
import com.cognora.platform.tenant.service.ITenantConfigService;
import com.cognora.platform.tenant.support.PlatformAdminGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantConfigServiceImpl extends ServiceImpl<TenantConfigMapper, TenantConfig> implements ITenantConfigService {

    public static final String SCOPE_TENANT = "TENANT";
    public static final String SCOPE_PLATFORM = "PLATFORM";

    private final PlatformAdminGuard platformAdminGuard;

    @Override
    public TenantConfig createConfig(TenantConfig entity) {
        String currentTenant = currentTenantId();
        String requestedTenant = entity.getTenantId();
        if (!StringUtils.hasText(requestedTenant)) {
            entity.setTenantId(currentTenant);
        } else if (!requestedTenant.equals(currentTenant) && !platformAdminGuard.isPlatformAdmin()) {
            throw new AccessDeniedException("无权为其他租户创建配置");
        }
        log.info("创建租户配置: tenantId={}, configKey={}", entity.getTenantId(), entity.getConfigKey());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        save(entity);
        log.info("租户配置创建成功: id={}", entity.getId());
        return entity;
    }

    @Override
    public TenantConfig getById(String id) {
        TenantConfig config = baseMapper.selectOne(Wrappers.<TenantConfig>lambdaQuery()
                .eq(TenantConfig::getId, id));
        assertReadable(config);
        return config;
    }

    @Override
    public TenantConfig updateConfig(TenantConfig entity) {
        TenantConfig existing = baseMapper.selectOne(Wrappers.<TenantConfig>lambdaQuery()
                .eq(TenantConfig::getId, entity.getId()));
        if (existing == null) {
            throw new IllegalArgumentException("租户配置不存在: " + entity.getId());
        }
        assertReadable(existing);
        entity.setTenantId(existing.getTenantId());
        log.info("更新租户配置: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    @Override
    public boolean deleteById(String id) {
        TenantConfig existing = baseMapper.selectOne(Wrappers.<TenantConfig>lambdaQuery()
                .eq(TenantConfig::getId, id));
        if (existing == null) {
            return false;
        }
        assertReadable(existing);
        log.info("删除租户配置: id={}", id);
        return removeById(id);
    }

    @Override
    public TenantConfig getConfigByTenantAndKey(String tenantId, String configKey) {
        if (!Objects.equals(tenantId, currentTenantId()) && !platformAdminGuard.isPlatformAdmin()) {
            throw new AccessDeniedException("无权访问其他租户的配置");
        }
        log.debug("查询租户配置: tenantId={}, configKey={}", tenantId, configKey);
        return baseMapper.selectOne(Wrappers.<TenantConfig>lambdaQuery()
                .eq(TenantConfig::getTenantId, tenantId)
                .eq(TenantConfig::getConfigKey, configKey));
    }

    @Override
    public Page<TenantConfig> pageQuery(int page, int size, String tenantId, String configCategory,
                                        String configKey, String scope) {
        String effectiveTenantId = resolveTenantFilter(scope, tenantId);
        log.debug("分页查询租户配置: page={}, size={}, tenantId={}, scope={}", page, size, effectiveTenantId, scope);

        LambdaQueryWrapper<TenantConfig> wrapper = Wrappers.lambdaQuery();
        if (effectiveTenantId == null) {
            wrapper.isNull(TenantConfig::getTenantId);
        } else {
            wrapper.eq(TenantConfig::getTenantId, effectiveTenantId);
        }
        if (StringUtils.hasText(configCategory)) {
            wrapper.eq(TenantConfig::getConfigCategory, configCategory);
        }
        if (StringUtils.hasText(configKey)) {
            wrapper.like(TenantConfig::getConfigKey, configKey);
        }
        wrapper.orderByDesc(TenantConfig::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    /**
     * 解析分页查询的租户过滤条件。
     *
     * <p>scope=PLATFORM 时只返回平台级配置（{@code tenant_id IS NULL}），并要求调用方是
     * 平台管理员；scope=TENANT（默认）时永远强制为当前租户，忽略请求里传入的 tenantId，
     * 避免不带 tenantId 或传入他租户 ID 时读到其他租户的数据。</p>
     */
    String resolveTenantFilter(String scope, String requestedTenantId) {
        if (!StringUtils.hasText(scope) || SCOPE_TENANT.equalsIgnoreCase(scope)) {
            return currentTenantId();
        }
        if (SCOPE_PLATFORM.equalsIgnoreCase(scope)) {
            if (!platformAdminGuard.isPlatformAdmin()) {
                throw new AccessDeniedException("平台级配置仅平台管理员可查询");
            }
            return null;
        }
        throw new IllegalArgumentException("scope 仅支持 TENANT 或 PLATFORM: " + scope);
    }

    private void assertReadable(TenantConfig config) {
        if (config == null || platformAdminGuard.isPlatformAdmin()) {
            return;
        }
        if (config.getTenantId() == null) {
            throw new AccessDeniedException("平台级配置仅平台管理员可访问");
        }
        if (!Objects.equals(config.getTenantId(), currentTenantId())) {
            throw new AccessDeniedException("无权访问其他租户的配置");
        }
    }

    private String currentTenantId() {
        return TenantContextHolder.getRequiredTenantId().toString();
    }
}
