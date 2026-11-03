package com.scmcloud.notify.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.notify.domain.entity.SysUserNotificationPreference;
import com.scmcloud.notify.mapper.SysUserNotificationPreferenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserNotificationPreferenceCommandService {

    private final SysUserNotificationPreferenceMapper sysUserNotificationPreferenceMapper;

    @Master(reason = "创建用户通知偏好")
    @Transactional(rollbackFor = Exception.class)
    public SysUserNotificationPreference createPreference(SysUserNotificationPreference entity) {
        log.info("创建用户通知偏好: userId={}, notificationType={}, channel={}",
                entity.getUserId(), entity.getNotificationType(), entity.getChannel());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getEnabled() == null) {
            entity.setEnabled(true);
        }
        sysUserNotificationPreferenceMapper.insert(entity);
        log.info("用户通知偏好创建成功: id={}", entity.getId());
        return entity;
    }

    @Master(reason = "更新用户通知偏好")
    @Transactional(rollbackFor = Exception.class)
    public SysUserNotificationPreference updatePreference(SysUserNotificationPreference entity) {
        log.info("更新用户通知偏好: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        sysUserNotificationPreferenceMapper.updateById(entity);
        return entity;
    }

    @Master(reason = "删除用户通知偏好")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(String id) {
        log.info("删除用户通知偏好: id={}", id);
        return sysUserNotificationPreferenceMapper.deleteById(id) > 0;
    }
}
