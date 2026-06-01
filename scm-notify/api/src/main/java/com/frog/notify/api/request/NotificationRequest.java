package com.frog.notify.api.request;

import java.util.Map;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 通知请求
 */
@Data
@Accessors(chain = true)
public class NotificationRequest {

    private Long userId;
    private String notifyType;
    private String channel;
    private String title;
    private String content;
    private String templateCode;
    private Map<String, String> templateParams;
}
