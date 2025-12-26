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
 * 用户通知偏好表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_user_notification_preference")
@Schema(description = "用户通知偏好表")
public class SysUserNotificationPreference implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @Schema(description = "用户ID(跨库关联db_user.sys_user)")
    @TableField("user_id")
    private String userId;

    @Schema(description = "通知类型:APPROVAL,SECURITY,SYSTEM,MARKETING")
    @TableField("notification_type")
    private String notificationType;

    @TableField("channel")
    private String channel;

    @TableField("enabled")
    private Boolean enabled;

    @Schema(description = "免打扰开始时间")
    @TableField("quiet_hours_start")
    private LocalDateTime quietHoursStart;

    @Schema(description = "免打扰结束时间")
    @TableField("quiet_hours_end")
    private LocalDateTime quietHoursEnd;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;


}
