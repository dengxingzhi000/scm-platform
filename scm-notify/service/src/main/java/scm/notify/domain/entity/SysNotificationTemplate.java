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
 * 通知模板表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_notification_template")
public class SysNotificationTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

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

    @TableField("variables_schema")
    private String variablesSchema;

    @TableField("status")
    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("deleted")
    private Boolean deleted;

}
