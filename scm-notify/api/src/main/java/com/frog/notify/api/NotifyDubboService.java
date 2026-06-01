package com.frog.notify.api;

import com.frog.notify.api.dto.BatchNotifyResult;
import com.frog.notify.api.dto.NotifyResult;
import com.frog.notify.api.request.BatchNotificationRequest;
import com.frog.notify.api.request.NotificationRequest;

/**
 * 通知服务 Dubbo 接口
 *
 * <p>提供单条/批量通知发送等核心功能，供其他微服务通过 RPC 调用。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface NotifyDubboService {

    /**
     * 发送单条通知
     *
     * @param request 通知请求
     * @return 发送结果
     */
    NotifyResult sendNotification(NotificationRequest request);

    /**
     * 发送批量通知
     *
     * @param request 批量通知请求
     * @return 发送结果
     */
    BatchNotifyResult sendBatchNotification(BatchNotificationRequest request);
}
