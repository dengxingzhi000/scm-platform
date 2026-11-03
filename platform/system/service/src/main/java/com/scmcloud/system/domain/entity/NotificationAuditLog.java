package com.scmcloud.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 閫氱煡鍙戦€佸璁¤〃
 *
 * @author Deng
 * @since 2025-12-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "sys_notification_audit", autoResultMap = true)
public class NotificationAuditLog {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("reference_id")
    private String referenceId;

    @TableField("user_id")
    private UUID userId;

    @TableField("channel")
    private String channel;

    @TableField("status")
    private String status;

    @TableField("subject")
    private String subject;

    @TableField("username")
    private String username;

    @TableField("email")
    private String email;

    @TableField("phone")
    private String phone;

    @TableField("template_code")
    private String templateCode;

    @TableField("content")
    private String content;

    @TableField(value = "variables", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> variables;

    @TableField("error_message")
    private String errorMessage;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("max_retries")
    private Integer maxRetries;

    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;

    @TableField("sent_at")
    private LocalDateTime sentAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // ==================== 鍐椾綑瀛楁 ====================

    @TableField("real_name")
    private String realName;

    /**
     * 閫氱煡娓犻亾鏋氫妇
     */
    @Getter
    public enum Channel {
        EMAIL("EMAIL", "Email"),
        SMS("SMS", "SMS"),
        WECHAT("WECHAT", "WeChat"),
        DINGTALK("DINGTALK", "DingTalk"),
        FEISHU("FEISHU", "Feishu"),
        PUSH("PUSH", "Push");
    
        private final String code;
        private final String desc;

        Channel(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }

    /**
     * 鍙戦€佺姸鎬佹灇锟?
     */
    @Getter
    public enum Status {
        PENDING("PENDING", "Pending"),
        SENT("SENT", "Sent"),
        FAILED("FAILED", "Failed"),
        CANCELLED("CANCELLED", "Cancelled");

        private final String code;
        private final String desc;

        Status(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}