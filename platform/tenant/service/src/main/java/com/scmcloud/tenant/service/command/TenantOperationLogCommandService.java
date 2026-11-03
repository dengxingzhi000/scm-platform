package com.scmcloud.tenant.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.tenant.domain.entity.TenantOperationLog;
import com.scmcloud.tenant.mapper.TenantOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantOperationLogCommandService {

    private final TenantOperationLogMapper tenantOperationLogMapper;

    @Master(reason = "创建租户操作日志")
    @Transactional(rollbackFor = Exception.class)
    public TenantOperationLog createLog(TenantOperationLog entity) {
        log.info("创建租户操作日志: tenantId={}, operationType={}", entity.getTenantId(), entity.getOperationType());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        tenantOperationLogMapper.insert(entity);
        log.info("租户操作日志创建成功: id={}", entity.getId());
        return entity;
    }

    @Master(reason = "删除租户操作日志")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(String id) {
        log.info("删除租户操作日志: id={}", id);
        return tenantOperationLogMapper.deleteById(id) > 0;
    }
}
