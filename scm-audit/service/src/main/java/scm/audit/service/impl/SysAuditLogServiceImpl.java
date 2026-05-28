package scm.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import scm.audit.domain.entity.SysAuditLog;
import scm.audit.mapper.SysAuditLogMapper;
import scm.audit.service.ISysAuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SysAuditLogServiceImpl
        extends ServiceImpl<SysAuditLogMapper, SysAuditLog>
        implements ISysAuditLogService {

    public SysAuditLog createLog(SysAuditLog entity) {
        log.info("创建审计日志: userId={}, operationType={}, module={}",
                entity.getUserId(), entity.getOperationType(), entity.getOperationModule());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        save(entity);
        log.info("审计日志创建成功: id={}", entity.getId());
        return entity;
    }

    public SysAuditLog getById(String id) {
        return lambdaQuery()
                .eq(SysAuditLog::getId, id)
                .one();
    }

    public boolean deleteById(String id) {
        log.info("删除审计日志: id={}", id);
        return removeById(id);
    }

    public List<SysAuditLog> listByUserId(String userId) {
        return lambdaQuery()
                .eq(SysAuditLog::getUserId, userId)
                .orderByDesc(SysAuditLog::getCreateTime)
                .list();
    }

    public List<SysAuditLog> listByModule(String module) {
        return lambdaQuery()
                .eq(SysAuditLog::getOperationModule, module)
                .orderByDesc(SysAuditLog::getCreateTime)
                .list();
    }

    public List<SysAuditLog> listByBusinessType(String businessType) {
        return lambdaQuery()
                .eq(SysAuditLog::getBusinessType, businessType)
                .orderByDesc(SysAuditLog::getCreateTime)
                .list();
    }

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
        return page(pageParam, wrapper);
    }
}
