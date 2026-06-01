package scm.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户通知偏好�?
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_user_notification_preference")
public class SysUserNotificationPreference implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("notification_type")
    private String notificationType;

    @TableField("channel")
    private String channel;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("quiet_hours_start")
    private LocalDateTime quietHoursStart;

    @TableField("quiet_hours_end")
    private LocalDateTime quietHoursEnd;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

}
