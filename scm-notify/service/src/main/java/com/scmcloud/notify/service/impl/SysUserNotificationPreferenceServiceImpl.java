package com.scmcloud.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.scmcloud.notify.domain.entity.SysUserNotificationPreference;
import com.scmcloud.notify.mapper.SysUserNotificationPreferenceMapper;
import com.scmcloud.notify.service.ISysUserNotificationPreferenceService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SysUserNotificationPreferenceServiceImpl
        extends ServiceImpl<SysUserNotificationPreferenceMapper, SysUserNotificationPreference>
        implements ISysUserNotificationPreferenceService {

    @Override
    public SysUserNotificationPreference createPreference(SysUserNotificationPreference entity) {
        log.info("创建用户通知偏好: userId={}, notificationType={}, channel={}",
                (entity.getUserId() + "").replace("\r", "_").replace("\n", "_"), (entity.getNotificationType() + "").replace("\r", "_").replace("\n", "_"), (entity.getChannel() + "").replace("\r", "_").replace("\n", "_"));
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getEnabled() == null) {
            entity.setEnabled(true);
        }
        save(entity);
        log.info("用户通知偏好创建成功: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        return entity;
    }

    @Override
    public SysUserNotificationPreference getById(String id) {
        return lambdaQuery()
                .eq(SysUserNotificationPreference::getId, id)
                .one();
    }

    @Override
    public SysUserNotificationPreference updatePreference(SysUserNotificationPreference entity) {
        log.info("更新用户通知偏好: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    @Override
    public boolean deleteById(String id) {
        log.info("删除用户通知偏好: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return removeById(id);
    }

    @Override
    public List<SysUserNotificationPreference> getByUserId(String userId) {
        return lambdaQuery()
                .eq(SysUserNotificationPreference::getUserId, userId)
                .list();
    }

    @Override
    public Page<SysUserNotificationPreference> pageQuery(int page, int size, String userId,
                                                          String notificationType, String channel) {
        log.debug("分页查询用户通知偏好: page={}, size={}, userId={}, notificationType={}, channel={}",
                (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (userId + "").replace("\r", "_").replace("\n", "_"), (notificationType + "").replace("\r", "_").replace("\n", "_"), (channel + "").replace("\r", "_").replace("\n", "_"));

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

        Page<SysUserNotificationPreference> pageParam = new Page<>(page, size);
        return page(pageParam, wrapper);
    }
}
