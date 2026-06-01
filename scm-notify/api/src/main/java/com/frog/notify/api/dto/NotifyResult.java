package com.frog.notify.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 通知发送结果
 */
@Data
@Accessors(chain = true)
public class NotifyResult {

    private Long notifyId;
    private boolean success;
    private String message;
}
