package com.scmcloud.audit.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.audit.domain.entity.SysAuditLog;
import com.scmcloud.audit.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysAuditLogCommandService {
    private final SysAuditLogMapper sysAuditLogMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public SysAuditLog createLog(SysAuditLog entity) {
        log.info("创建审计日志: userId={}, operationType={}, module={}",
                entity.getUserId(), entity.getOperationType(), entity.getOperationModule());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        sysAuditLogMapper.insert(entity);
        log.info("审计日志创建成功: id={}", entity.getId());
        return entity;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(String id) {
        log.info("删除审计日志: id={}", id);
        return sysAuditLogMapper.deleteById(id) > 0;
    }
}
