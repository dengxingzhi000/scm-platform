package com.scmcloud.tenant.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.tenant.domain.entity.TenantSubscription;
import com.scmcloud.tenant.mapper.TenantSubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSubscriptionQueryService {

    private final TenantSubscriptionMapper tenantSubscriptionMapper;

    @Slave
    public TenantSubscription getById(String id) {
        LambdaQueryWrapper<TenantSubscription> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TenantSubscription::getId, id);
        return tenantSubscriptionMapper.selectOne(wrapper);
    }

    @Slave
    public List<TenantSubscription> listByTenantId(String tenantId) {
        LambdaQueryWrapper<TenantSubscription> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TenantSubscription::getTenantId, tenantId)
                .orderByDesc(TenantSubscription::getCreateTime);
        return tenantSubscriptionMapper.selectList(wrapper);
    }

    @Slave
    public Page<TenantSubscription> pageQuery(int page, int size, String tenantId, Integer status) {
        LambdaQueryWrapper<TenantSubscription> wrapper = Wrappers.lambdaQuery();

        if (tenantId != null) {
            wrapper.eq(TenantSubscription::getTenantId, tenantId);
        }
        if (status != null) {
            wrapper.eq(TenantSubscription::getStatus, status);
        }
        wrapper.orderByDesc(TenantSubscription::getCreateTime);

        return tenantSubscriptionMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
