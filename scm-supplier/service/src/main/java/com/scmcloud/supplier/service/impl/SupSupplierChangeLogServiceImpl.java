package com.scmcloud.supplier.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.supplier.domain.entity.SupSupplierChangeLog;
import com.scmcloud.supplier.mapper.SupSupplierChangeLogMapper;
import com.scmcloud.supplier.service.ISupSupplierChangeLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SupSupplierChangeLogServiceImpl
        extends ServiceImpl<SupSupplierChangeLogMapper, SupSupplierChangeLog>
        implements ISupSupplierChangeLogService {

    @Override
    public void record(String supplierId, String changeType, String changeContent,
                       String reason, String operatorId, String operatorName) {
        SupSupplierChangeLog changeLog = new SupSupplierChangeLog()
                .setId(UUID.randomUUID().toString())
                .setSupplierId(supplierId)
                .setChangeType(changeType)
                .setChangeContent(changeContent)
                .setReason(reason)
                .setOperatorId(operatorId)
                .setOperatorName(operatorName)
                .setCreateTime(LocalDateTime.now());
        save(changeLog);
    }

    @Override
    public List<SupSupplierChangeLog> listBySupplierId(String supplierId, int limit) {
        int bounded = Math.max(1, Math.min(limit, 100));
        return lambdaQuery()
                .eq(SupSupplierChangeLog::getSupplierId, supplierId)
                .orderByDesc(SupSupplierChangeLog::getCreateTime)
                .last("LIMIT " + bounded)
                .list();
    }
}
