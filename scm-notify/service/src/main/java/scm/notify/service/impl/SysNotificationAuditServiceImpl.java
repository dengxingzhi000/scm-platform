package scm.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import scm.notify.domain.entity.SysNotificationAudit;
import scm.notify.mapper.SysNotificationAuditMapper;
import scm.notify.service.ISysNotificationAuditService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SysNotificationAuditServiceImpl extends ServiceImpl<SysNotificationAuditMapper, SysNotificationAudit>
        implements ISysNotificationAuditService {

    public SysNotificationAudit createAudit(SysNotificationAudit entity) {
        log.info("创建通知审计: userId={}, channel={}, templateCode={}", entity.getUserId(), entity.getChannel(), entity.getTemplateCode());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreatedAt(LocalDateTime.now());
        if (entity.getStatus() == null) {
            entity.setStatus("PENDING");
        }
        if (entity.getRetryCount() == null) {
            entity.setRetryCount(0);
        }
        if (entity.getMaxRetries() == null) {
            entity.setMaxRetries(3);
        }
        save(entity);
        log.info("通知审计创建成功: id={}", entity.getId());
        return entity;
    }

    public SysNotificationAudit getById(String id) {
        return lambdaQuery()
                .eq(SysNotificationAudit::getId, id)
                .one();
    }

    public boolean deleteById(String id) {
        log.info("删除通知审计: id={}", id);
        return removeById(id);
    }

    public List<SysNotificationAudit> listByUserId(String userId) {
        return lambdaQuery()
                .eq(SysNotificationAudit::getUserId, userId)
                .orderByDesc(SysNotificationAudit::getCreatedAt)
                .list();
    }

    public List<SysNotificationAudit> listByStatus(String status) {
        return lambdaQuery()
                .eq(SysNotificationAudit::getStatus, status)
                .orderByDesc(SysNotificationAudit::getCreatedAt)
                .list();
    }

    public boolean sendNotification(String id) {
        log.info("发送通知: id={}", id);
        SysNotificationAudit audit = getById(id);
        if (audit == null) {
            log.warn("通知审计不存在: id={}", id);
            return false;
        }
        audit.setStatus("SENT");
        audit.setSentAt(LocalDateTime.now());
        boolean success = updateById(audit);
        if (success) {
            log.info("通知发送成功: id={}", id);
        }
        return success;
    }

    public Page<SysNotificationAudit> pageQuery(int page, int size, String userId,
                                                 String channel, String status) {
        log.debug("分页查询通知审计: page={}, size={}, userId={}, channel={}, status={}",
                page, size, userId, channel, status);

        LambdaQueryWrapper<SysNotificationAudit> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(userId)) {
            wrapper.eq(SysNotificationAudit::getUserId, userId);
        }
        if (StringUtils.hasText(channel)) {
            wrapper.eq(SysNotificationAudit::getChannel, channel);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysNotificationAudit::getStatus, status);
        }
        wrapper.orderByDesc(SysNotificationAudit::getCreatedAt);

        Page<SysNotificationAudit> pageParam = new Page<>(page, size);
        return page(pageParam, wrapper);
    }
}
