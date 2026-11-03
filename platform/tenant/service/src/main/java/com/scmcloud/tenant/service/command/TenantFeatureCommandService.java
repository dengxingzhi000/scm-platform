package com.scmcloud.tenant.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.tenant.domain.entity.TenantFeature;
import com.scmcloud.tenant.mapper.TenantFeatureMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantFeatureCommandService {

    private final TenantFeatureMapper tenantFeatureMapper;

    @Master(reason = "创建租户功能")
    @Transactional(rollbackFor = Exception.class)
    public TenantFeature createFeature(TenantFeature entity) {
        log.info("创建租户功能: tenantId={}, featureCode={}", entity.getTenantId(), entity.getFeatureCode());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getEnabled() == null) {
            entity.setEnabled(true);
        }
        tenantFeatureMapper.insert(entity);
        log.info("租户功能创建成功: id={}", entity.getId());
        return entity;
    }

    @Master(reason = "更新租户功能")
    @Transactional(rollbackFor = Exception.class)
    public TenantFeature updateFeature(TenantFeature entity) {
        log.info("更新租户功能: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        tenantFeatureMapper.updateById(entity);
        return entity;
    }

    @Master(reason = "删除租户功能")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(String id) {
        log.info("删除租户功能: id={}", id);
        return tenantFeatureMapper.deleteById(id) > 0;
    }
}
