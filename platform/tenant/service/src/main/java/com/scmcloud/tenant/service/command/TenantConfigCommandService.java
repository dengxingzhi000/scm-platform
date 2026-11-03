package com.scmcloud.tenant.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.tenant.domain.entity.TenantConfig;
import com.scmcloud.tenant.mapper.TenantConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantConfigCommandService {

    private final TenantConfigMapper tenantConfigMapper;

    @Master(reason = "创建租户配置")
    @Transactional(rollbackFor = Exception.class)
    public TenantConfig createConfig(TenantConfig entity) {
        log.info("创建租户配置: tenantId={}, configKey={}", entity.getTenantId(), entity.getConfigKey());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        tenantConfigMapper.insert(entity);
        log.info("租户配置创建成功: id={}", entity.getId());
        return entity;
    }

    @Master(reason = "更新租户配置")
    @Transactional(rollbackFor = Exception.class)
    public TenantConfig updateConfig(TenantConfig entity) {
        log.info("更新租户配置: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        tenantConfigMapper.updateById(entity);
        return entity;
    }

    @Master(reason = "删除租户配置")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(String id) {
        log.info("删除租户配置: id={}", id);
        return tenantConfigMapper.deleteById(id) > 0;
    }
}
