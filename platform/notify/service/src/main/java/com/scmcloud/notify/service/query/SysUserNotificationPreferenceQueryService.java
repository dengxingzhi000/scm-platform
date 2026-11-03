package com.scmcloud.notify.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.notify.domain.entity.SysUserNotificationPreference;
import com.scmcloud.notify.mapper.SysUserNotificationPreferenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserNotificationPreferenceQueryService {

    private final SysUserNotificationPreferenceMapper sysUserNotificationPreferenceMapper;

    @Slave
    public SysUserNotificationPreference getById(String id) {
        LambdaQueryWrapper<SysUserNotificationPreference> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUserNotificationPreference::getId, id);
        return sysUserNotificationPreferenceMapper.selectOne(wrapper);
    }

    @Slave
    public List<SysUserNotificationPreference> getByUserId(String userId) {
        LambdaQueryWrapper<SysUserNotificationPreference> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUserNotificationPreference::getUserId, userId);
        return sysUserNotificationPreferenceMapper.selectList(wrapper);
    }

    @Slave
    public Page<SysUserNotificationPreference> pageQuery(int page, int size, String userId,
                                                          String notificationType, String channel) {
        LambdaQueryWrapper<SysUserNotificationPreference> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(userId)) {
            wrapper.eq(SysUserNotificationPreference::getUserId, userId);
        }
        if (StringUtils.hasText(notificationType)) {
            wrapper.eq(SysUserNotificationPreference::getNotificationType, notificationType);
        }
        if (StringUtils.hasText(channel)) {
            wrapper.eq(SysUserNotificationPreference::getChannel, channel);
        }
        wrapper.orderByDesc(SysUserNotificationPreference::getCreateTime);

        return sysUserNotificationPreferenceMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
