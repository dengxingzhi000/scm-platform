package com.cognora.platform.notify.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.platform.notify.domain.entity.SysUserNotification;

import java.time.LocalDateTime;
import java.util.List;

/**
 * All query and update operations are scoped to the authenticated user and the current tenant;
 * callers cannot address another user's notifications.
 */
public interface ISysUserNotificationService extends IService<SysUserNotification> {

    Page<SysUserNotification> pageList(int page, int size, String type, Boolean isRead,
                                       String keyword, LocalDateTime startTime, LocalDateTime endTime);

    long unreadCount();

    boolean markAsRead(String id);

    boolean markAllAsRead();

    boolean deleteNotification(String id);

    int deleteNotifications(List<String> ids);

    /**
     * Creates a notification for {@code userId} unless {@code businessKey} was already delivered
     * to that user in the tenant. Returns true when a row was inserted.
     */
    boolean createIfAbsent(String tenantId, String userId, String title, String content,
                           String type, String link, String businessKey);
}
