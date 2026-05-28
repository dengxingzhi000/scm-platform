package scm.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import scm.audit.domain.entity.SysSensitiveOperationLog;
import scm.audit.mapper.SysSensitiveOperationLogMapper;
import scm.audit.service.ISysSensitiveOperationLogService;
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

    public SysSensitiveOperationLog createLog(SysSensitiveOperationLog entity) {
        log.info("创建敏感操作日志: userId={}, operationType={}", entity.getUserId(), entity.getOperationType());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        save(entity);
        log.info("敏感操作日志创建成功: id={}", entity.getId());
        return entity;
    }

    public SysSensitiveOperationLog getById(String id) {
        return lambdaQuery()
                .eq(SysSensitiveOperationLog::getId, id)
                .one();
    }

    public boolean deleteById(String id) {
        log.info("删除敏感操作日志: id={}", id);
        return removeById(id);
    }

    public List<SysSensitiveOperationLog> listByUserId(String userId) {
        return lambdaQuery()
                .eq(SysSensitiveOperationLog::getUserId, userId)
                .orderByDesc(SysSensitiveOperationLog::getCreateTime)
                .list();
    }

    public List<SysSensitiveOperationLog> listByRiskLevel(Integer riskScore) {
        return lambdaQuery()
                .eq(SysSensitiveOperationLog::getRiskScore, riskScore)
                .orderByDesc(SysSensitiveOperationLog::getCreateTime)
                .list();
    }

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
        return page(pageParam, wrapper);
    }
}
