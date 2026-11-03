package com.scmcloud.audit.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.audit.domain.entity.SysAuditLog;
import com.scmcloud.audit.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysAuditLogQueryService {
    private final SysAuditLogMapper sysAuditLogMapper;

    @Slave
    public SysAuditLog getById(String id) {
        LambdaQueryWrapper<SysAuditLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysAuditLog::getId, id);
        return sysAuditLogMapper.selectOne(wrapper);
    }

    @Slave
    public List<SysAuditLog> listByUserId(String userId) {
        LambdaQueryWrapper<SysAuditLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysAuditLog::getUserId, userId)
                .orderByDesc(SysAuditLog::getCreateTime);
        return sysAuditLogMapper.selectList(wrapper);
    }

    @Slave
    public List<SysAuditLog> listByModule(String module) {
        LambdaQueryWrapper<SysAuditLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysAuditLog::getOperationModule, module)
                .orderByDesc(SysAuditLog::getCreateTime);
        return sysAuditLogMapper.selectList(wrapper);
    }

    @Slave
    public List<SysAuditLog> listByBusinessType(String businessType) {
        LambdaQueryWrapper<SysAuditLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysAuditLog::getBusinessType, businessType)
                .orderByDesc(SysAuditLog::getCreateTime);
        return sysAuditLogMapper.selectList(wrapper);
    }

    @Slave
    public Page<SysAuditLog> pageQuery(int page, int size, String userId, String operationType,
                                        String module, String businessType, Integer riskLevel) {
        log.debug("分页查询审计日志: page={}, size={}, userId={}, operationType={}, module={}, businessType={}, riskLevel={}",
                page, size, userId, operationType, module, businessType, riskLevel);

        LambdaQueryWrapper<SysAuditLog> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(userId)) {
            wrapper.eq(SysAuditLog::getUserId, userId);
        }
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(SysAuditLog::getOperationType, operationType);
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(SysAuditLog::getOperationModule, module);
        }
        if (StringUtils.hasText(businessType)) {
            wrapper.eq(SysAuditLog::getBusinessType, businessType);
        }
        if (riskLevel != null) {
            wrapper.eq(SysAuditLog::getRiskLevel, riskLevel);
        }
        wrapper.orderByDesc(SysAuditLog::getCreateTime);

        Page<SysAuditLog> pageParam = new Page<>(page, size);
        return sysAuditLogMapper.selectPage(pageParam, wrapper);
    }
}
