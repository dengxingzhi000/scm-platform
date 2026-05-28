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
 * 操作审计日志表(按月分区)
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_audit_log")
@Schema(description = "操作审计日志表(按月分区)")
public class SysAuditLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @Schema(description = "用户ID(跨库关联db_user.sys_user)")
    @TableField("user_id")
    private String userId;

    @TableField("username")
    private String username;

    @TableField("real_name")
    private String realName;

    @Schema(description = "部门ID(跨库关联db_org.sys_dept)")
    @TableField("dept_id")
    private String deptId;

    @Schema(description = "操作类型:LOGIN,LOGOUT,ADD,UPDATE,DELETE,QUERY,EXPORT,APPROVE")
    @TableField("operation_type")
    private String operationType;

    @TableField("operation_module")
    private String operationModule;

    @TableField("operation_desc")
    private String operationDesc;

    @TableField("request_uri")
    private String requestUri;

    @TableField("request_method")
    private String requestMethod;

    @TableField("request_params")
    private String requestParams;

    @TableField("response_data")
    private String responseData;

    @TableField("response_status")
    private Integer responseStatus;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("location")
    private String location;

    @TableField("user_agent")
    private String userAgent;

    @TableField("business_type")
    private String businessType;

    @TableField("business_id")
    private String businessId;

    @TableField("old_value")
    private String oldValue;

    @TableField("new_value")
    private String newValue;

    @Schema(description = "风险等级:1-低,2-中,3-高,4-极高")
    @TableField("risk_level")
    private Integer riskLevel;

    @TableField("status")
    private Integer status;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("execute_time")
    private Integer executeTime;

    @TableField("create_time")
    private LocalDateTime createTime;

}
