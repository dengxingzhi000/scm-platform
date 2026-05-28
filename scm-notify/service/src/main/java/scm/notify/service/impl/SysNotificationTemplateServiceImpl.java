package scm.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import scm.notify.domain.entity.SysNotificationTemplate;
import scm.notify.mapper.SysNotificationTemplateMapper;
import scm.notify.service.ISysNotificationTemplateService;
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

    public SysNotificationTemplate createTemplate(SysNotificationTemplate entity) {
        log.info("创建通知模板: templateCode={}, channel={}", entity.getTemplateCode(), entity.getChannel());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleted(false);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        save(entity);
        log.info("通知模板创建成功: id={}", entity.getId());
        return entity;
    }

    public SysNotificationTemplate getById(String id) {
        return lambdaQuery()
                .eq(SysNotificationTemplate::getId, id)
                .eq(SysNotificationTemplate::getDeleted, false)
                .one();
    }

    public SysNotificationTemplate updateTemplate(SysNotificationTemplate entity) {
        log.info("更新通知模板: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    public boolean deleteById(String id) {
        log.info("删除通知模板: id={}", id);
        SysNotificationTemplate template = new SysNotificationTemplate();
        template.setId(id);
        template.setDeleted(true);
        template.setUpdateTime(LocalDateTime.now());
        return updateById(template);
    }

    public List<SysNotificationTemplate> listByType(String channel) {
        return lambdaQuery()
                .eq(SysNotificationTemplate::getChannel, channel)
                .eq(SysNotificationTemplate::getDeleted, false)
                .orderByDesc(SysNotificationTemplate::getCreateTime)
                .list();
    }

    public boolean enableTemplate(String id) {
        log.info("启用通知模板: id={}", id);
        SysNotificationTemplate template = new SysNotificationTemplate();
        template.setId(id);
        template.setStatus(1);
        template.setUpdateTime(LocalDateTime.now());
        return updateById(template);
    }

    public boolean disableTemplate(String id) {
        log.info("禁用通知模板: id={}", id);
        SysNotificationTemplate template = new SysNotificationTemplate();
        template.setId(id);
        template.setStatus(0);
        template.setUpdateTime(LocalDateTime.now());
        return updateById(template);
    }

    public Page<SysNotificationTemplate> pageQuery(int page, int size, String channel,
                                                    String templateCode, Integer status) {
        log.debug("分页查询通知模板: page={}, size={}, channel={}, templateCode={}, status={}",
                page, size, channel, templateCode, status);

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
