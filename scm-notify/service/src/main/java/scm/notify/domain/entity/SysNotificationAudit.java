package scm.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 通知发送审计表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_notification_audit")
@Schema(description = "通知发送审计表")
public class SysNotificationAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("reference_id")
    private String referenceId;

    @Schema(description = "接收用户ID(跨库关联db_user.sys_user)")
    @TableField("user_id")
    private String userId;

    @Schema(description = "通知渠道:EMAIL,SMS,WECHAT,DINGTALK,FEISHU,PUSH")
    @TableField("channel")
    private String channel;

    @Schema(description = "发送状态:PENDING,SENT,FAILED,CANCELLED")
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

    @Schema(description = "模板变量(JSONB格式)")
    @TableField("variables")
    private String variables;

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

    @TableField("created_at")
    private LocalDateTime createdAt;

    @Schema(description = "冗余字段：用户真实姓名")
    @TableField("real_name")
    private String realName;


}
