package com.scmcloud.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.scmcloud.tenant.domain.entity.TenantConfig;
import com.scmcloud.tenant.mapper.TenantConfigMapper;
import com.scmcloud.tenant.service.ITenantConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class TenantConfigServiceImpl extends ServiceImpl<TenantConfigMapper, TenantConfig> implements ITenantConfigService {

    @Override
    public TenantConfig createConfig(TenantConfig entity) {
        log.info("创建租户配置: tenantId={}, configKey={}", (entity.getTenantId() + "").replace("\r", "_").replace("\n", "_"), (entity.getConfigKey() + "").replace("\r", "_").replace("\n", "_"));
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        save(entity);
        log.info("租户配置创建成功: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        return entity;
    }

    @Override
    public TenantConfig getById(String id) {
        return lambdaQuery()
                .eq(TenantConfig::getId, id)
                .one();
    }

    @Override
    public TenantConfig updateConfig(TenantConfig entity) {
        log.info("更新租户配置: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    @Override
    public boolean deleteById(String id) {
        log.info("删除租户配置: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return removeById(id);
    }

    @Override
    public TenantConfig getConfigByTenantAndKey(String tenantId, String configKey) {
        log.debug("查询租户配置: tenantId={}, configKey={}", (tenantId + "").replace("\r", "_").replace("\n", "_"), (configKey + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .eq(TenantConfig::getTenantId, tenantId)
                .eq(TenantConfig::getConfigKey, configKey)
                .one();
    }

    @Override
    public Page<TenantConfig> pageQuery(int page, int size, String tenantId, String configCategory, String configKey) {
        log.debug("分页查询租户配置: page={}, size={}, tenantId={}, configCategory={}, configKey={}",
                (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (tenantId + "").replace("\r", "_").replace("\n", "_"), (configCategory + "").replace("\r", "_").replace("\n", "_"), (configKey + "").replace("\r", "_").replace("\n", "_"));

        LambdaQueryWrapper<TenantConfig> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(tenantId)) {
            wrapper.eq(TenantConfig::getTenantId, tenantId);
        }
        if (StringUtils.hasText(configCategory)) {
            wrapper.eq(TenantConfig::getConfigCategory, configCategory);
        }
        if (StringUtils.hasText(configKey)) {
            wrapper.like(TenantConfig::getConfigKey, configKey);
        }
        wrapper.orderByDesc(TenantConfig::getCreateTime);

        Page<TenantConfig> pageParam = new Page<>(page, size);
        return page(pageParam, wrapper);
    }
}
