package scm.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import scm.tenant.domain.entity.TenantSubscription;
import scm.tenant.mapper.TenantSubscriptionMapper;
import scm.tenant.service.ITenantSubscriptionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TenantSubscriptionServiceImpl extends ServiceImpl<TenantSubscriptionMapper, TenantSubscription> implements ITenantSubscriptionService {

    public TenantSubscription createSubscription(TenantSubscription entity) {
        log.info("创建租户订阅: tenantId={}, packageId={}", entity.getTenantId(), entity.getPackageId());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        save(entity);
        log.info("租户订阅创建成功: id={}", entity.getId());
        return entity;
    }

    public TenantSubscription getById(String id) {
        return lambdaQuery()
                .eq(TenantSubscription::getId, id)
                .one();
    }

    public TenantSubscription updateSubscription(TenantSubscription entity) {
        log.info("更新租户订阅: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    public boolean deleteById(String id) {
        log.info("删除租户订阅: id={}", id);
        return removeById(id);
    }

    public boolean subscribe(String tenantId, String packageId) {
        log.info("租户订阅: tenantId={}, packageId={}", tenantId, packageId);
        TenantSubscription subscription = new TenantSubscription();
        subscription.setId(UUID.randomUUID().toString());
        subscription.setTenantId(tenantId);
        subscription.setPackageId(packageId);
        subscription.setStatus(1);
        subscription.setCreateTime(LocalDateTime.now());
        subscription.setUpdateTime(LocalDateTime.now());
        boolean success = save(subscription);
        if (success) {
            log.info("租户订阅成功: id={}", subscription.getId());
        }
        return success;
    }

    public boolean unsubscribe(String id) {
        log.info("取消租户订阅: id={}", id);
        TenantSubscription subscription = new TenantSubscription();
        subscription.setId(id);
        subscription.setStatus(3);
        subscription.setUpdateTime(LocalDateTime.now());
        boolean success = updateById(subscription);
        if (success) {
            log.info("取消租户订阅成功: id={}", id);
        }
        return success;
    }

    public List<TenantSubscription> listByTenantId(String tenantId) {
        return lambdaQuery()
                .eq(TenantSubscription::getTenantId, tenantId)
                .orderByDesc(TenantSubscription::getCreateTime)
                .list();
    }

    public Page<TenantSubscription> pageQuery(int page, int size, String tenantId, Integer status) {
        log.debug("分页查询租户订阅: page={}, size={}, tenantId={}, status={}", page, size, tenantId, status);

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
