package scm.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import scm.tenant.domain.entity.TenantConfig;
import scm.tenant.mapper.TenantConfigMapper;
import scm.tenant.service.ITenantConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TenantConfigServiceImpl extends ServiceImpl<TenantConfigMapper, TenantConfig> implements ITenantConfigService {

    public TenantConfig createConfig(TenantConfig entity) {
        log.info("创建租户配置: tenantId={}, configKey={}", entity.getTenantId(), entity.getConfigKey());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        save(entity);
        log.info("租户配置创建成功: id={}", entity.getId());
        return entity;
    }

    public TenantConfig getById(String id) {
        return lambdaQuery()
                .eq(TenantConfig::getId, id)
                .one();
    }

    public TenantConfig updateConfig(TenantConfig entity) {
        log.info("更新租户配置: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    public boolean deleteById(String id) {
        log.info("删除租户配置: id={}", id);
        return removeById(id);
    }

    public TenantConfig getConfigByTenantAndKey(String tenantId, String configKey) {
        log.debug("查询租户配置: tenantId={}, configKey={}", tenantId, configKey);
        return lambdaQuery()
                .eq(TenantConfig::getTenantId, tenantId)
                .eq(TenantConfig::getConfigKey, configKey)
                .one();
    }

    public Page<TenantConfig> pageQuery(int page, int size, String tenantId, String configCategory, String configKey) {
        log.debug("分页查询租户配置: page={}, size={}, tenantId={}, configCategory={}, configKey={}",
                page, size, tenantId, configCategory, configKey);

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
