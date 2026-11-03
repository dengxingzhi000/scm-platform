package com.scmcloud.notify.api.request;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 批量通知请求
 */
@Data
@Accessors(chain = true)
public class BatchNotificationRequest {

    private List<Long> userIds;
    private String notifyType;
    private String channel;
    private String title;
    private String content;
    private String templateCode;
    private Map<String, String> templateParams;
}
