package com.scmcloud.notify.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.notify.domain.entity.SysNotificationTemplate;
import com.scmcloud.notify.mapper.SysNotificationTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysNotificationTemplateCommandService {

    private final SysNotificationTemplateMapper sysNotificationTemplateMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "创建通知模板")
    @Transactional(rollbackFor = Exception.class)
    public SysNotificationTemplate createTemplate(SysNotificationTemplate entity) {
        log.info("创建通知模板: templateCode={}, channel={}", entity.getTemplateCode(), entity.getChannel());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleted(false);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        sysNotificationTemplateMapper.insert(entity);
        log.info("通知模板创建成功: id={}", entity.getId());
        return entity;
    }

    @Master(reason = "更新通知模板")
    @Transactional(rollbackFor = Exception.class)
    public SysNotificationTemplate updateTemplate(SysNotificationTemplate entity) {
        log.info("更新通知模板: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        sysNotificationTemplateMapper.updateById(entity);
        return entity;
    }

    @Master(reason = "删除通知模板")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(String id) {
        log.info("删除通知模板: id={}", id);
        SysNotificationTemplate template = new SysNotificationTemplate();
        template.setId(id);
        template.setDeleted(true);
        template.setUpdateTime(LocalDateTime.now());
        return sysNotificationTemplateMapper.updateById(template) > 0;
    }

    @Master(reason = "启用通知模板")
    @Transactional(rollbackFor = Exception.class)
    public boolean enableTemplate(String id) {
        log.info("启用通知模板: id={}", id);
        SysNotificationTemplate current = sysNotificationTemplateMapper.selectById(id);
        String fromStatus = current.getStatus() == 0 ? "DISABLED" : "ENABLED";
        statusValidator.validateTransition("NOTIFICATION", fromStatus, "ENABLED");

        SysNotificationTemplate template = new SysNotificationTemplate();
        template.setId(id);
        template.setStatus(1);
        template.setUpdateTime(LocalDateTime.now());
        return sysNotificationTemplateMapper.updateById(template) > 0;
    }

    @Master(reason = "禁用通知模板")
    @Transactional(rollbackFor = Exception.class)
    public boolean disableTemplate(String id) {
        log.info("禁用通知模板: id={}", id);
        SysNotificationTemplate current = sysNotificationTemplateMapper.selectById(id);
        String fromStatus = current.getStatus() == 0 ? "DISABLED" : "ENABLED";
        statusValidator.validateTransition("NOTIFICATION", fromStatus, "DISABLED");

        SysNotificationTemplate template = new SysNotificationTemplate();
        template.setId(id);
        template.setStatus(0);
        template.setUpdateTime(LocalDateTime.now());
        return sysNotificationTemplateMapper.updateById(template) > 0;
    }
}
