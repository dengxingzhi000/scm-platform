package com.scmcloud.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.scmcloud.tenant.domain.entity.TenantSubscription;
import com.scmcloud.tenant.mapper.TenantSubscriptionMapper;
import com.scmcloud.tenant.service.ITenantSubscriptionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TenantSubscriptionServiceImpl extends ServiceImpl<TenantSubscriptionMapper, TenantSubscription>
        implements ITenantSubscriptionService {

    @Override
    public TenantSubscription createSubscription(TenantSubscription entity) {
        log.info("创建租户订阅: tenantId={}, packageId={}", (entity.getTenantId() + "").replace("\r", "_").replace("\n", "_"), (entity.getPackageId() + "").replace("\r", "_").replace("\n", "_"));
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        save(entity);
        log.info("租户订阅创建成功: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        return entity;
    }

    @Override
    public TenantSubscription getById(String id) {
        return lambdaQuery()
                .eq(TenantSubscription::getId, id)
                .one();
    }

    @Override
    public TenantSubscription updateSubscription(TenantSubscription entity) {
        log.info("更新租户订阅: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    @Override
    public boolean deleteById(String id) {
        log.info("删除租户订阅: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return removeById(id);
    }

    @Override
    public boolean subscribe(String tenantId, String packageId) {
        log.info("租户订阅: tenantId={}, packageId={}", (tenantId + "").replace("\r", "_").replace("\n", "_"), (packageId + "").replace("\r", "_").replace("\n", "_"));
        TenantSubscription subscription = new TenantSubscription();
        subscription.setId(UUID.randomUUID().toString());
        subscription.setTenantId(tenantId);
        subscription.setPackageId(packageId);
        subscription.setStatus(1);
        subscription.setCreateTime(LocalDateTime.now());
        subscription.setUpdateTime(LocalDateTime.now());
        boolean success = save(subscription);
        if (success) {
            log.info("租户订阅成功: id={}", (subscription.getId() + "").replace("\r", "_").replace("\n", "_"));
        }
        return success;
    }

    @Override
    public boolean unsubscribe(String id) {
        log.info("取消租户订阅: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        TenantSubscription subscription = new TenantSubscription();
        subscription.setId(id);
        subscription.setStatus(3);
        subscription.setUpdateTime(LocalDateTime.now());
        boolean success = updateById(subscription);
        if (success) {
            log.info("取消租户订阅成功: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        }
        return success;
    }

    @Override
    public List<TenantSubscription> listByTenantId(String tenantId) {
        return lambdaQuery()
                .eq(TenantSubscription::getTenantId, tenantId)
                .orderByDesc(TenantSubscription::getCreateTime)
                .list();
    }

    @Override
    public Page<TenantSubscription> pageQuery(int page, int size, String tenantId, Integer status) {
        log.debug("分页查询租户订阅: page={}, size={}, tenantId={}, status={}", (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (tenantId + "").replace("\r", "_").replace("\n", "_"), (status + "").replace("\r", "_").replace("\n", "_"));

        LambdaQueryWrapper<TenantSubscription> wrapper = Wrappers.lambdaQuery();

        if (tenantId != null) {
            wrapper.eq(TenantSubscription::getTenantId, tenantId);
        }
        if (status != null) {
            wrapper.eq(TenantSubscription::getStatus, status);
        }
        wrapper.orderByDesc(TenantSubscription::getCreateTime);

        Page<TenantSubscription> pageParam = new Page<>(page, size);
        return page(pageParam, wrapper);
    }
}
