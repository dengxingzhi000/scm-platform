package scm.audit.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serial;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 敏感操作日志表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_sensitive_operation_log")
@Schema(description = "敏感操作日志表")
public class SysSensitiveOperationLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @Schema(description = "用户ID(跨库关联db_user.sys_user)")
    @TableField("user_id")
    private String userId;

    @TableField("username")
    private String username;

    @Schema(description = "操作类型:EXPORT,BULK_UPDATE,BULK_DELETE,DATA_DOWNLOAD,PERMISSION_CHANGE")
    @TableField("operation_type")
    private String operationType;

    @TableField("operation_module")
    private String operationModule;

    @Schema(description = "敏感数据类型:PERSONAL_INFO,FINANCIAL,MEDICAL,SECRET")
    @TableField("sensitive_data_type")
    private String sensitiveDataType;

    @TableField("data_fingerprint")
    private String dataFingerprint;

    @TableField("affected_count")
    private Integer affectedCount;

    @TableField("target_table")
    private String targetTable;

    @TableField("target_ids")
    private String targetIds;

    @TableField("operation_detail")
    private String operationDetail;

    @TableField("approval_required")
    private Boolean approvalRequired;

    @Schema(description = "审批ID(跨库关联db_approval.sys_permission_approval)")
    @TableField("approval_id")
    private String approvalId;

    @Schema(description = "风险评分:1-10")
    @TableField("risk_score")
    private Integer riskScore;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("user_agent")
    private String userAgent;

    @TableField("device_fingerprint")
    private String deviceFingerprint;

    @TableField("location")
    private String location;

    @TableField("create_time")
    private LocalDateTime createTime;

    @Schema(description = "冗余字段：用户真实姓名")
    @TableField("real_name")
    private String realName;

    @Schema(description = "冗余字段：部门名称")
    @TableField("dept_name")
    private String deptName;

}
