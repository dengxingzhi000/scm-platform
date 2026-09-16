package com.scmcloud.notify.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 閫氱煡鍙戦€佺粨锟?
 */
@Data
@Accessors(chain = true)
public class NotifyResult {

    private Long notifyId;
    private boolean success;
    private String message;
}
