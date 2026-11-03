package com.scmcloud.audit.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.audit.domain.entity.SysSensitiveOperationLog;
import com.scmcloud.audit.mapper.SysSensitiveOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysSensitiveOperationLogQueryService {
    private final SysSensitiveOperationLogMapper sysSensitiveOperationLogMapper;

    @Slave
    public SysSensitiveOperationLog getById(String id) {
        LambdaQueryWrapper<SysSensitiveOperationLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysSensitiveOperationLog::getId, id);
        return sysSensitiveOperationLogMapper.selectOne(wrapper);
    }

    @Slave
    public List<SysSensitiveOperationLog> listByUserId(String userId) {
        LambdaQueryWrapper<SysSensitiveOperationLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysSensitiveOperationLog::getUserId, userId)
                .orderByDesc(SysSensitiveOperationLog::getCreateTime);
        return sysSensitiveOperationLogMapper.selectList(wrapper);
    }

    @Slave
    public List<SysSensitiveOperationLog> listByRiskLevel(Integer riskScore) {
        LambdaQueryWrapper<SysSensitiveOperationLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysSensitiveOperationLog::getRiskScore, riskScore)
                .orderByDesc(SysSensitiveOperationLog::getCreateTime);
        return sysSensitiveOperationLogMapper.selectList(wrapper);
    }

    @Slave
    public Page<SysSensitiveOperationLog> pageQuery(int page, int size, String userId,
                                                     String operationType, Integer riskScore) {
        log.debug("分页查询敏感操作日志: page={}, size={}, userId={}, operationType={}, riskScore={}",
                page, size, userId, operationType, riskScore);

        LambdaQueryWrapper<SysSensitiveOperationLog> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(userId)) {
            wrapper.eq(SysSensitiveOperationLog::getUserId, userId);
        }
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(SysSensitiveOperationLog::getOperationType, operationType);
        }
        if (riskScore != null) {
            wrapper.eq(SysSensitiveOperationLog::getRiskScore, riskScore);
        }
        wrapper.orderByDesc(SysSensitiveOperationLog::getCreateTime);

        Page<SysSensitiveOperationLog> pageParam = new Page<>(page, size);
        return sysSensitiveOperationLogMapper.selectPage(pageParam, wrapper);
    }
}
