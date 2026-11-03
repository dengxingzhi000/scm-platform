package com.scmcloud.notify.api;

import com.scmcloud.notify.api.dto.BatchNotifyResult;
import com.scmcloud.notify.api.dto.NotifyResult;
import com.scmcloud.notify.api.request.BatchNotificationRequest;
import com.scmcloud.notify.api.request.NotificationRequest;

/**
 * 閫氱煡鏈嶅姟 Dubbo 鎺ュ彛
 *
 * <p>鎻愪緵鍗曟潯/鎵归噺閫氱煡鍙戦€佺瓑鏍稿績鍔熻兘锛屼緵鍏朵粬寰湇鍔¢€氳繃 RPC 璋冪敤锟?
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface NotifyDubboService {

    /**
     * 鍙戦€佸崟鏉¢€氱煡
     *
     * @param request 閫氱煡璇锋眰
     * @return 鍙戦€佺粨锟?
     */
    NotifyResult sendNotification(NotificationRequest request);

    /**
     * 鍙戦€佹壒閲忛€氱煡
     *
     * @param request 鎵归噺閫氱煡璇锋眰
     * @return 鍙戦€佺粨锟?
     */
    BatchNotifyResult sendBatchNotification(BatchNotificationRequest request);
}
