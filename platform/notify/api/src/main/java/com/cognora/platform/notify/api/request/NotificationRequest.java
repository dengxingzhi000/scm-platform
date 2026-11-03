package com.cognora.platform.notify.api.request;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 站内通知发送请求（Kafka 事件体）。
 *
 * <p>生产方通常在批处理或定时任务中调用，线程上没有请求上下文，
 * 因此租户与收件人都必须显式给出。
 */
@Data
@Accessors(chain = true)
public class NotificationRequest {

    /** 收件人所属租户，必填。 */
    private String tenantId;

    /** 收件人用户 ID（UUID 字符串），必填。 */
    private String userId;

    /** 通知类型：SYSTEM / ORDER / INVENTORY / APPROVAL / SECURITY，缺省按 SYSTEM 处理。 */
    private String notifyType;

    private String title;

    private String content;

    /** 点击通知后跳转的前端路由。 */
    private String link;

    /** 业务事件标识；同一用户重复消费同一条事件不会产生第二条通知。 */
    private String businessKey;
}
