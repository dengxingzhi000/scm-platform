package com.scmcloud.tenant.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.tenant.domain.entity.TenantOperationLog;
import com.scmcloud.tenant.mapper.TenantOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantOperationLogQueryService {

    private final TenantOperationLogMapper tenantOperationLogMapper;

    @Slave
    public TenantOperationLog getById(String id) {
        LambdaQueryWrapper<TenantOperationLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TenantOperationLog::getId, id);
        return tenantOperationLogMapper.selectOne(wrapper);
    }

    @Slave
    public List<TenantOperationLog> listByTenantId(String tenantId) {
        LambdaQueryWrapper<TenantOperationLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TenantOperationLog::getTenantId, tenantId)
                .orderByDesc(TenantOperationLog::getCreateTime);
        return tenantOperationLogMapper.selectList(wrapper);
    }

    @Slave
    public List<TenantOperationLog> listByOperationType(String operationType) {
        LambdaQueryWrapper<TenantOperationLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TenantOperationLog::getOperationType, operationType)
                .orderByDesc(TenantOperationLog::getCreateTime);
        return tenantOperationLogMapper.selectList(wrapper);
    }

    @Slave
    public Page<TenantOperationLog> pageQuery(int page, int size, String tenantId, String operationType,
                                               String operationModule) {
        LambdaQueryWrapper<TenantOperationLog> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(tenantId)) {
            wrapper.eq(TenantOperationLog::getTenantId, tenantId);
        }
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(TenantOperationLog::getOperationType, operationType);
        }
        if (StringUtils.hasText(operationModule)) {
            wrapper.eq(TenantOperationLog::getOperationModule, operationModule);
        }
        wrapper.orderByDesc(TenantOperationLog::getCreateTime);

        return tenantOperationLogMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
