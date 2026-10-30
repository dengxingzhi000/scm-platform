package com.scmcloud.notify.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.notify.domain.entity.SysUserNotification;

/**
 * All query and update operations are scoped to the authenticated user and the current tenant;
 * callers cannot address another user's notifications.
 */
public interface ISysUserNotificationService extends IService<SysUserNotification> {

    Page<SysUserNotification> pageList(int page, int size, String type, Boolean isRead);

    long unreadCount();

    boolean markAsRead(String id);

    boolean markAllAsRead();

    /**
     * Creates a notification for {@code userId} unless {@code businessKey} was already delivered
     * to that user in the tenant. Returns true when a row was inserted.
     */
    boolean createIfAbsent(String tenantId, String userId, String title, String content,
                           String type, String link, String businessKey);
}
