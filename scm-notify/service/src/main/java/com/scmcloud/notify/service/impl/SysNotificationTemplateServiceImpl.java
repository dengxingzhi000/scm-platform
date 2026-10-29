package com.scmcloud.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.scmcloud.notify.domain.entity.SysNotificationTemplate;
import com.scmcloud.notify.mapper.SysNotificationTemplateMapper;
import com.scmcloud.notify.service.ISysNotificationTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SysNotificationTemplateServiceImpl
        extends ServiceImpl<SysNotificationTemplateMapper, SysNotificationTemplate>
        implements ISysNotificationTemplateService {

    @Override
    public SysNotificationTemplate createTemplate(SysNotificationTemplate entity) {
        log.info("创建通知模板: templateCode={}, channel={}", (entity.getTemplateCode() + "").replace("\r", "_").replace("\n", "_"), (entity.getChannel() + "").replace("\r", "_").replace("\n", "_"));
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleted(false);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        save(entity);
        log.info("通知模板创建成功: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        return entity;
    }

    @Override
    public SysNotificationTemplate getById(String id) {
        return lambdaQuery()
                .eq(SysNotificationTemplate::getId, id)
                .eq(SysNotificationTemplate::getDeleted, false)
                .one();
    }

    @Override
    public SysNotificationTemplate updateTemplate(SysNotificationTemplate entity) {
        log.info("更新通知模板: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    @Override
    public boolean deleteById(String id) {
        log.info("删除通知模板: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        SysNotificationTemplate template = new SysNotificationTemplate();
        template.setId(id);
        template.setDeleted(true);
        template.setUpdateTime(LocalDateTime.now());
        return updateById(template);
    }

    @Override
    public List<SysNotificationTemplate> listByType(String channel) {
        return lambdaQuery()
                .eq(SysNotificationTemplate::getChannel, channel)
                .eq(SysNotificationTemplate::getDeleted, false)
                .orderByDesc(SysNotificationTemplate::getCreateTime)
                .list();
    }

    @Override
    public boolean enableTemplate(String id) {
        log.info("启用通知模板: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        SysNotificationTemplate template = new SysNotificationTemplate();
        template.setId(id);
        template.setStatus(1);
        template.setUpdateTime(LocalDateTime.now());
        return updateById(template);
    }

    @Override
    public boolean disableTemplate(String id) {
        log.info("禁用通知模板: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        SysNotificationTemplate template = new SysNotificationTemplate();
        template.setId(id);
        template.setStatus(0);
        template.setUpdateTime(LocalDateTime.now());
        return updateById(template);
    }

    @Override
    public Page<SysNotificationTemplate> pageQuery(int page, int size, String channel,
                                                    String templateCode, Integer status) {
        log.debug("分页查询通知模板: page={}, size={}, channel={}, templateCode={}, status={}",
                (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (channel + "").replace("\r", "_").replace("\n", "_"), (templateCode + "").replace("\r", "_").replace("\n", "_"), (status + "").replace("\r", "_").replace("\n", "_"));

        LambdaQueryWrapper<SysNotificationTemplate> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(channel)) {
            wrapper.eq(SysNotificationTemplate::getChannel, channel);
        }
        if (StringUtils.hasText(templateCode)) {
            wrapper.like(SysNotificationTemplate::getTemplateCode, templateCode);
        }
        if (status != null) {
            wrapper.eq(SysNotificationTemplate::getStatus, status);
        }
        wrapper.eq(SysNotificationTemplate::getDeleted, false);
        wrapper.orderByDesc(SysNotificationTemplate::getCreateTime);

        Page<SysNotificationTemplate> pageParam = new Page<>(page, size);
        return page(pageParam, wrapper);
    }
}
