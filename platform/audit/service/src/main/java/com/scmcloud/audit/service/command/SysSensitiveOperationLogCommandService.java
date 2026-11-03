package com.scmcloud.audit.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.audit.domain.entity.SysSensitiveOperationLog;
import com.scmcloud.audit.mapper.SysSensitiveOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysSensitiveOperationLogCommandService {
    private final SysSensitiveOperationLogMapper sysSensitiveOperationLogMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public SysSensitiveOperationLog createLog(SysSensitiveOperationLog entity) {
        log.info("创建敏感操作日志: userId={}, operationType={}", entity.getUserId(), entity.getOperationType());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        sysSensitiveOperationLogMapper.insert(entity);
        log.info("敏感操作日志创建成功: id={}", entity.getId());
        return entity;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(String id) {
        log.info("删除敏感操作日志: id={}", id);
        return sysSensitiveOperationLogMapper.deleteById(id) > 0;
    }
}
