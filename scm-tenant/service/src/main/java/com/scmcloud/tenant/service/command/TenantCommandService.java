package com.scmcloud.tenant.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.tenant.domain.entity.Tenant;
import com.scmcloud.tenant.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantCommandService {
    private final TenantMapper tenantMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "创建租户")
    @Transactional(rollbackFor = Exception.class)
    public Tenant createTenant(Tenant entity) {
        log.info("创建租户: tenantCode={}, tenantName={}", entity.getTenantCode(), entity.getTenantName());
        entity.setId(UUIDv7Util.generateString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleted(false);
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        if (tenantMapper.insert(entity) <= 0) {
            log.warn("租户插入未生效: tenantCode={}", entity.getTenantCode());
        } else {
            log.info("租户创建成功: id={}", entity.getId());
        }
        return entity;
    }

    @Master(reason = "更新租户")
    @Transactional(rollbackFor = Exception.class)
    public Tenant updateTenant(Tenant entity) {
        log.info("更新租户: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        if (tenantMapper.updateById(entity) <= 0) {
            log.warn("租户更新未生效: id={}", entity.getId());
        }
        return entity;
    }

    @Master(reason = "删除租户")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(String id) {
        log.info("删除租户: id={}", id);
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setDeleted(true);
        tenant.setUpdateTime(LocalDateTime.now());
        return tenantMapper.updateById(tenant) > 0;
    }

    @Master(reason = "启用租户")
    @Transactional(rollbackFor = Exception.class)
    public boolean enableTenant(String id) {
        log.info("启用租户: id={}", id);
        Tenant current = tenantMapper.selectById(id);
        String fromStatus = tenantStatusName(current.getStatus());
        statusValidator.validateTransition("TENANT", fromStatus, "ACTIVE");

        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setStatus(1);
        tenant.setActivatedAt(LocalDateTime.now());
        tenant.setUpdateTime(LocalDateTime.now());
        return tenantMapper.updateById(tenant) > 0;
    }

    @Master(reason = "禁用租户")
    @Transactional(rollbackFor = Exception.class)
    public boolean disableTenant(String id) {
        log.info("禁用租户: id={}", id);
        Tenant current = tenantMapper.selectById(id);
        String fromStatus = tenantStatusName(current.getStatus());
        statusValidator.validateTransition("TENANT", fromStatus, "SUSPENDED");

        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setStatus(2);
        tenant.setSuspendedAt(LocalDateTime.now());
        tenant.setUpdateTime(LocalDateTime.now());
        return tenantMapper.updateById(tenant) > 0;
    }

    private String tenantStatusName(Integer status) {
        return switch (status) {
            case 0 -> "TRIAL";
            case 1 -> "ACTIVE";
            case 2 -> "SUSPENDED";
            case 3 -> "EXPIRED";
            default -> String.valueOf(status);
        };
    }
}
