package com.scmcloud.notify.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.notify.domain.entity.SysNotificationTemplate;
import com.scmcloud.notify.mapper.SysNotificationTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysNotificationTemplateQueryService {

    private final SysNotificationTemplateMapper sysNotificationTemplateMapper;

    @Slave
    public SysNotificationTemplate getById(String id) {
        LambdaQueryWrapper<SysNotificationTemplate> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysNotificationTemplate::getId, id)
                .eq(SysNotificationTemplate::getDeleted, false);
        return sysNotificationTemplateMapper.selectOne(wrapper);
    }

    @Slave
    public List<SysNotificationTemplate> listByType(String channel) {
        LambdaQueryWrapper<SysNotificationTemplate> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysNotificationTemplate::getChannel, channel)
                .eq(SysNotificationTemplate::getDeleted, false)
                .orderByDesc(SysNotificationTemplate::getCreateTime);
        return sysNotificationTemplateMapper.selectList(wrapper);
    }

    @Slave
    public Page<SysNotificationTemplate> pageQuery(int page, int size, String channel,
                                                    String templateCode, Integer status) {
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

        return sysNotificationTemplateMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
