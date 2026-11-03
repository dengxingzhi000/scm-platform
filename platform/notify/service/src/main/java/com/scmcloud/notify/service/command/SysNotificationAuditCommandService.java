package com.scmcloud.notify.service.command;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.notify.domain.entity.SysNotificationAudit;
import com.scmcloud.notify.mapper.SysNotificationAuditMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysNotificationAuditCommandService {

    private final SysNotificationAuditMapper sysNotificationAuditMapper;

    @Master(reason = "创建通知审计")
    @Transactional(rollbackFor = Exception.class)
    public SysNotificationAudit createAudit(SysNotificationAudit entity) {
        log.info("创建通知审计: userId={}, channel={}, templateCode={}", entity.getUserId(), entity.getChannel(),
                entity.getTemplateCode());
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
        sysNotificationAuditMapper.insert(entity);
        log.info("通知审计创建成功: id={}", entity.getId());
        return entity;
    }

    @Master(reason = "删除通知审计")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(String id) {
        log.info("删除通知审计: id={}", id);
        return sysNotificationAuditMapper.deleteById(id) > 0;
    }

    @Master(reason = "发送通知")
    @Transactional(rollbackFor = Exception.class)
    public boolean sendNotification(String id) {
        log.info("发送通知: id={}", id);
        LambdaQueryWrapper<SysNotificationAudit> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysNotificationAudit::getId, id);
        SysNotificationAudit audit = sysNotificationAuditMapper.selectOne(wrapper);
        if (audit == null) {
            log.warn("通知审计不存在: id={}", id);
            return false;
        }
        audit.setStatus("SENT");
        audit.setSentAt(LocalDateTime.now());
        boolean success = sysNotificationAuditMapper.updateById(audit) > 0;
        if (success) {
            log.info("通知发送成功: id={}", id);
        }
        return success;
    }
}
