package com.scmcloud.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.scmcloud.tenant.domain.entity.TenantOperationLog;
import com.scmcloud.tenant.mapper.TenantOperationLogMapper;
import com.scmcloud.tenant.service.ITenantOperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TenantOperationLogServiceImpl extends ServiceImpl<TenantOperationLogMapper, TenantOperationLog>
        implements ITenantOperationLogService {

    @Override
    public TenantOperationLog createLog(TenantOperationLog entity) {
        log.info("创建租户操作日志: tenantId={}, operationType={}", (entity.getTenantId() + "").replace("\r", "_").replace("\n", "_"), (entity.getOperationType() + "").replace("\r", "_").replace("\n", "_"));
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        save(entity);
        log.info("租户操作日志创建成功: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        return entity;
    }

    @Override
    public TenantOperationLog getById(String id) {
        return lambdaQuery()
                .eq(TenantOperationLog::getId, id)
                .one();
    }

    @Override
    public boolean deleteById(String id) {
        log.info("删除租户操作日志: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return removeById(id);
    }

    @Override
    public List<TenantOperationLog> listByTenantId(String tenantId) {
        return lambdaQuery()
                .eq(TenantOperationLog::getTenantId, tenantId)
                .orderByDesc(TenantOperationLog::getCreateTime)
                .list();
    }

    @Override
    public List<TenantOperationLog> listByOperationType(String operationType) {
        return lambdaQuery()
                .eq(TenantOperationLog::getOperationType, operationType)
                .orderByDesc(TenantOperationLog::getCreateTime)
                .list();
    }

    @Override
    public Page<TenantOperationLog> pageQuery(int page, int size, String tenantId, String operationType,
                                               String operationModule) {
        log.debug("分页查询租户操作日志: page={}, size={}, tenantId={}, operationType={}, operationModule={}",
                (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (tenantId + "").replace("\r", "_").replace("\n", "_"), (operationType + "").replace("\r", "_").replace("\n", "_"), (operationModule + "").replace("\r", "_").replace("\n", "_"));

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

        Page<TenantOperationLog> pageParam = new Page<>(page, size);
        return page(pageParam, wrapper);
    }
}
