package com.cognora.platform.tenant.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.platform.tenant.domain.entity.TenantConfig;
import com.cognora.platform.tenant.mapper.TenantConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantConfigQueryService {

    private final TenantConfigMapper tenantConfigMapper;

    @Slave
    public TenantConfig getById(String id) {
        LambdaQueryWrapper<TenantConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TenantConfig::getId, id);
        return tenantConfigMapper.selectOne(wrapper);
    }

    @Slave
    public TenantConfig getConfigByTenantAndKey(String tenantId, String configKey) {
        LambdaQueryWrapper<TenantConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TenantConfig::getTenantId, tenantId)
                .eq(TenantConfig::getConfigKey, configKey);
        return tenantConfigMapper.selectOne(wrapper);
    }

    @Slave
    public Page<TenantConfig> pageQuery(int page, int size, String tenantId, String configCategory, String configKey) {
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

        return tenantConfigMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
