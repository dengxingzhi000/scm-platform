package com.scmcloud.tenant.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.tenant.domain.entity.Tenant;
import com.scmcloud.tenant.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantQueryService {

    private final TenantMapper tenantMapper;

    @Slave
    public Tenant getById(String id) {
        LambdaQueryWrapper<Tenant> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Tenant::getId, id)
                .eq(Tenant::getDeleted, false);
        return tenantMapper.selectOne(wrapper);
    }

    @Slave
    public List<Tenant> listActive() {
        LambdaQueryWrapper<Tenant> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Tenant::getDeleted, false)
                .in(Tenant::getStatus, 0, 1)
                .orderByDesc(Tenant::getCreateTime);
        return tenantMapper.selectList(wrapper);
    }

    @Slave
    public Page<Tenant> pageQuery(int page, int size, String tenantName, Integer tenantType, Integer status) {
        LambdaQueryWrapper<Tenant> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(tenantName)) {
            wrapper.like(Tenant::getTenantName, tenantName);
        }
        if (tenantType != null) {
            wrapper.eq(Tenant::getTenantType, tenantType);
        }
        if (status != null) {
            wrapper.eq(Tenant::getStatus, status);
        }
        wrapper.eq(Tenant::getDeleted, false);
        wrapper.orderByDesc(Tenant::getCreateTime);

        return tenantMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
