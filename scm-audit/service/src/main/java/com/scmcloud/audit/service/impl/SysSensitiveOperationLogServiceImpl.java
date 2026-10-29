package com.scmcloud.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.scmcloud.audit.domain.entity.SysSensitiveOperationLog;
import com.scmcloud.audit.mapper.SysSensitiveOperationLogMapper;
import com.scmcloud.audit.service.ISysSensitiveOperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SysSensitiveOperationLogServiceImpl
        extends ServiceImpl<SysSensitiveOperationLogMapper, SysSensitiveOperationLog>
        implements ISysSensitiveOperationLogService {

    @Override
    public SysSensitiveOperationLog createLog(SysSensitiveOperationLog entity) {
        log.info("创建敏感操作日志: userId={}, operationType={}", (entity.getUserId() + "").replace("\r", "_").replace("\n", "_"), (entity.getOperationType() + "").replace("\r", "_").replace("\n", "_"));
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        save(entity);
        log.info("敏感操作日志创建成功: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        return entity;
    }

    @Override
    public SysSensitiveOperationLog getById(String id) {
        return lambdaQuery()
                .eq(SysSensitiveOperationLog::getId, id)
                .one();
    }

    @Override
    public boolean deleteById(String id) {
        log.info("删除敏感操作日志: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return removeById(id);
    }

    @Override
    public List<SysSensitiveOperationLog> listByUserId(String userId) {
        return lambdaQuery()
                .eq(SysSensitiveOperationLog::getUserId, userId)
                .orderByDesc(SysSensitiveOperationLog::getCreateTime)
                .list();
    }

    @Override
    public List<SysSensitiveOperationLog> listByRiskLevel(Integer riskScore) {
        return lambdaQuery()
                .eq(SysSensitiveOperationLog::getRiskScore, riskScore)
                .orderByDesc(SysSensitiveOperationLog::getCreateTime)
                .list();
    }

    @Override
    public Page<SysSensitiveOperationLog> pageQuery(int page, int size, String userId,
                                                     String operationType, Integer riskScore) {
        log.debug("分页查询敏感操作日志: page={}, size={}, userId={}, operationType={}, riskScore={}",
                (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (userId + "").replace("\r", "_").replace("\n", "_"), (operationType + "").replace("\r", "_").replace("\n", "_"), (riskScore + "").replace("\r", "_").replace("\n", "_"));

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
        return page(pageParam, wrapper);
    }
}
