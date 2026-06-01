package com.frog.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 通知模板表
 *
 * @author Deng
 * @since 2025-12-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "sys_notification_template", autoResultMap = true)
public class SysNotificationTemplate {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("template_code")
    private String templateCode;

    @TableField("template_name")
    private String templateName;

    @TableField("channel")
    private String channel;

    @TableField("subject_template")
    private String subjectTemplate;

    @TableField("content_template")
    private String contentTemplate;

    @TableField(value = "variables_schema", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> variablesSchema;

    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private UUID createBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private UUID updateBy;

    @TableLogic(value = "false", delval = "true")
    @TableField("deleted")
    private Boolean deleted;

    /**
     * 通知渠道枚举
     */
    @Getter
    public enum Channel {
        EMAIL("EMAIL", "邮件"),
        SMS("SMS", "短信"),
        WECHAT("WECHAT", "微信"),
        DINGTALK("DINGTALK", "钉钉"),
        FEISHU("FEISHU", "飞书"),
        PUSH("PUSH", "推送");
    
        private final String code;
        private final String desc;

        Channel(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}