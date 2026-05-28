package scm.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import scm.notify.domain.entity.SysUserNotificationPreference;
import scm.notify.mapper.SysUserNotificationPreferenceMapper;
import scm.notify.service.ISysUserNotificationPreferenceService;
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

    public SysUserNotificationPreference createPreference(SysUserNotificationPreference entity) {
        log.info("创建用户通知偏好: userId={}, notificationType={}, channel={}",
                entity.getUserId(), entity.getNotificationType(), entity.getChannel());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getEnabled() == null) {
            entity.setEnabled(true);
        }
        save(entity);
        log.info("用户通知偏好创建成功: id={}", entity.getId());
        return entity;
    }

    public SysUserNotificationPreference getById(String id) {
        return lambdaQuery()
                .eq(SysUserNotificationPreference::getId, id)
                .one();
    }

    public SysUserNotificationPreference updatePreference(SysUserNotificationPreference entity) {
        log.info("更新用户通知偏好: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    public boolean deleteById(String id) {
        log.info("删除用户通知偏好: id={}", id);
        return removeById(id);
    }

    public List<SysUserNotificationPreference> getByUserId(String userId) {
        return lambdaQuery()
                .eq(SysUserNotificationPreference::getUserId, userId)
                .list();
    }

    public Page<SysUserNotificationPreference> pageQuery(int page, int size, String userId,
                                                          String notificationType, String channel) {
        log.debug("分页查询用户通知偏好: page={}, size={}, userId={}, notificationType={}, channel={}",
                page, size, userId, notificationType, channel);

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
