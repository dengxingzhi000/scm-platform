package com.scmcloud.tenant.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.tenant.domain.entity.TenantSubscription;
import com.scmcloud.tenant.mapper.TenantSubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSubscriptionCommandService {

    private final TenantSubscriptionMapper tenantSubscriptionMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "创建租户订阅")
    @Transactional(rollbackFor = Exception.class)
    public TenantSubscription createSubscription(TenantSubscription entity) {
        log.info("创建租户订阅: tenantId={}, packageId={}", entity.getTenantId(), entity.getPackageId());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        tenantSubscriptionMapper.insert(entity);
        log.info("租户订阅创建成功: id={}", entity.getId());
        return entity;
    }

    @Master(reason = "更新租户订阅")
    @Transactional(rollbackFor = Exception.class)
    public TenantSubscription updateSubscription(TenantSubscription entity) {
        log.info("更新租户订阅: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        tenantSubscriptionMapper.updateById(entity);
        return entity;
    }

    @Master(reason = "删除租户订阅")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(String id) {
        log.info("删除租户订阅: id={}", id);
        return tenantSubscriptionMapper.deleteById(id) > 0;
    }

    @Master(reason = "租户订阅套餐")
    @Transactional(rollbackFor = Exception.class)
    public boolean subscribe(String tenantId, String packageId) {
        log.info("租户订阅: tenantId={}, packageId={}", tenantId, packageId);
        statusValidator.validateTransition("SUBSCRIPTION", "PENDING", "ACTIVE");

        TenantSubscription subscription = new TenantSubscription();
        subscription.setId(UUID.randomUUID().toString());
        subscription.setTenantId(tenantId);
        subscription.setPackageId(packageId);
        subscription.setStatus(1);
        subscription.setCreateTime(LocalDateTime.now());
        subscription.setUpdateTime(LocalDateTime.now());
        boolean success = tenantSubscriptionMapper.insert(subscription) > 0;
        if (success) {
            log.info("租户订阅成功: id={}", subscription.getId());
        }
        return success;
    }

    @Master(reason = "取消租户订阅")
    @Transactional(rollbackFor = Exception.class)
    public boolean unsubscribe(String id) {
        log.info("取消租户订阅: id={}", id);
        TenantSubscription current = tenantSubscriptionMapper.selectById(id);
        String fromStatus = subscriptionStatusName(current.getStatus());
        statusValidator.validateTransition("SUBSCRIPTION", fromStatus, "CANCELLED");

        TenantSubscription subscription = new TenantSubscription();
        subscription.setId(id);
        subscription.setStatus(3);
        subscription.setUpdateTime(LocalDateTime.now());
        boolean success = tenantSubscriptionMapper.updateById(subscription) > 0;
        if (success) {
            log.info("取消租户订阅成功: id={}", id);
        }
        return success;
    }

    private String subscriptionStatusName(Integer status) {
        return switch (status) {
            case 0 -> "PENDING";
            case 1 -> "ACTIVE";
            case 2 -> "EXPIRED";
            case 3 -> "CANCELLED";
            default -> String.valueOf(status);
        };
    }
}
