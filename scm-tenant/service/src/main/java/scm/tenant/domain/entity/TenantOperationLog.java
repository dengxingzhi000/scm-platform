package scm.tenant.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serial;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 租户操作日志表（分区？ * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tenant_operation_log")
public class TenantOperationLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("operation_type")
    private String operationType;

    @TableField("operation_module")
    private String operationModule;

    @TableField("operation_desc")
    private String operationDesc;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("operator_ip")
    private String operatorIp;

    @TableField("user_agent")
    private String userAgent;

    @TableField("request_url")
    private String requestUrl;

    @TableField("request_method")
    private String requestMethod;

    @TableField("request_params")
    private String requestParams;

    @TableField("response_status")
    private Integer responseStatus;

    @TableField("execution_time")
    private Integer executionTime;

    @TableField("create_time")
    private LocalDateTime createTime;

}
