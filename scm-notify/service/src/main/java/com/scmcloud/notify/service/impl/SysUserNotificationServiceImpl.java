package com.scmcloud.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.notify.domain.entity.SysUserNotification;
import com.scmcloud.notify.mapper.SysUserNotificationMapper;
import com.scmcloud.notify.service.ISysUserNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class SysUserNotificationServiceImpl
        extends ServiceImpl<SysUserNotificationMapper, SysUserNotification>
        implements ISysUserNotificationService {

    @Override
    public Page<SysUserNotification> pageList(int page, int size, String type, Boolean isRead) {
        String userId = currentUserId();
        log.debug("分页查询站内通知 page={}, size={}, userId={}, type={}, isRead={}",
                page, size, userId, type, isRead);

        LambdaQueryWrapper<SysUserNotification> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUserNotification::getTenantId, currentTenantId());
        wrapper.eq(SysUserNotification::getUserId, userId);
        wrapper.eq(SysUserNotification::getDeleted, false);
        if (StringUtils.hasText(type)) {
            wrapper.eq(SysUserNotification::getType, type);
        }
        if (isRead != null) {
            wrapper.eq(SysUserNotification::getIsRead, isRead);
        }
        wrapper.orderByAsc(SysUserNotification::getIsRead);
        wrapper.orderByDesc(SysUserNotification::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public long unreadCount() {
        LambdaQueryWrapper<SysUserNotification> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUserNotification::getTenantId, currentTenantId());
        wrapper.eq(SysUserNotification::getUserId, currentUserId());
        wrapper.eq(SysUserNotification::getDeleted, false);
        wrapper.eq(SysUserNotification::getIsRead, false);
        return count(wrapper);
    }

    @Override
    public boolean markAsRead(String id) {
        String userId = currentUserId();
        String tenantId = currentTenantId();

        boolean updated = lambdaUpdate()
                .eq(SysUserNotification::getId, id)
                .eq(SysUserNotification::getTenantId, tenantId)
                .eq(SysUserNotification::getUserId, userId)
                .eq(SysUserNotification::getIsRead, false)
                .set(SysUserNotification::getIsRead, true)
                .set(SysUserNotification::getReadAt, LocalDateTime.now())
                .set(SysUserNotification::getUpdateTime, LocalDateTime.now())
                .update();
        if (updated) {
            return true;
        }

        // Idempotent for an already-read notification that belongs to this user; false otherwise,
        // so another user's id is indistinguishable from a missing one.
        return lambdaQuery()
                .eq(SysUserNotification::getId, id)
                .eq(SysUserNotification::getTenantId, tenantId)
                .eq(SysUserNotification::getUserId, userId)
                .eq(SysUserNotification::getDeleted, false)
                .count() > 0;
    }

    @Override
    public boolean markAllAsRead() {
        return lambdaUpdate()
                .eq(SysUserNotification::getTenantId, currentTenantId())
                .eq(SysUserNotification::getUserId, currentUserId())
                .eq(SysUserNotification::getIsRead, false)
                .set(SysUserNotification::getIsRead, true)
                .set(SysUserNotification::getReadAt, LocalDateTime.now())
                .set(SysUserNotification::getUpdateTime, LocalDateTime.now())
                .update();
    }

    private String currentUserId() {
        UUID userId = SecurityUtils.requireCurrentUserUuid();
        return userId.toString();
    }

    private String currentTenantId() {
        return TenantContextHolder.getRequiredTenantId().toString();
    }

    @Override
    public boolean createIfAbsent(String tenantId, String userId, String title, String content,
                                  String type, String link, String businessKey) {
        if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(userId)) {
            return false;
        }
        if (businessKey != null && lambdaQuery()
                .eq(SysUserNotification::getTenantId, tenantId)
                .eq(SysUserNotification::getUserId, userId)
                .eq(SysUserNotification::getBusinessKey, businessKey)
                .count() > 0) {
            return false;
        }

        SysUserNotification notification = new SysUserNotification()
                .setId(UUID.randomUUID().toString())
                .setTenantId(tenantId)
                .setUserId(userId)
                .setTitle(title)
                .setContent(content)
                .setType(type)
                .setLink(link)
                .setBusinessKey(businessKey)
                .setIsRead(false)
                .setDeleted(false)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        try {
            return save(notification);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // The unique index on (tenant_id, user_id, business_key) settled a concurrent insert.
            return false;
        }
    }
}
