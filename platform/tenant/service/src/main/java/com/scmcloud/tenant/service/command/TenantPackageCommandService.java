package com.scmcloud.tenant.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.tenant.domain.entity.TenantPackage;
import com.scmcloud.tenant.mapper.TenantPackageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantPackageCommandService {

    private final TenantPackageMapper tenantPackageMapper;

    @Master(reason = "创建租户套餐")
    @Transactional(rollbackFor = Exception.class)
    public TenantPackage createPackage(TenantPackage entity) {
        log.info("创建租户套餐: packageCode={}, packageName={}", entity.getPackageCode(), entity.getPackageName());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleted(false);
        if (entity.getEnabled() == null) {
            entity.setEnabled(true);
        }
        tenantPackageMapper.insert(entity);
        log.info("租户套餐创建成功: id={}", entity.getId());
        return entity;
    }

    @Master(reason = "更新租户套餐")
    @Transactional(rollbackFor = Exception.class)
    public TenantPackage updatePackage(TenantPackage entity) {
        log.info("更新租户套餐: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        tenantPackageMapper.updateById(entity);
        return entity;
    }

    @Master(reason = "删除租户套餐")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(String id) {
        log.info("删除租户套餐: id={}", id);
        TenantPackage pkg = new TenantPackage();
        pkg.setId(id);
        pkg.setDeleted(true);
        pkg.setUpdateTime(LocalDateTime.now());
        return tenantPackageMapper.updateById(pkg) > 0;
    }
}
