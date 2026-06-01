package com.frog.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.frog.common.mybatisPlus.handler.UuidArrayTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 敏感操作日志表
 *
 * @author Deng
 * @since 2025-12-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "sys_sensitive_operation_log", autoResultMap = true)
public class SysSensitiveOperationLog {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    private UUID userId;

    private String username;

    private String operationType;

    private String operationModule;

    private String sensitiveDataType;

    private String dataFingerprint;

    private Integer affectedCount;

    private String targetTable;

    @TableField(typeHandler = UuidArrayTypeHandler.class)
    private UUID[] targetIds;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> operationDetail;

    private Boolean approvalRequired;

    private UUID approvalId;

    private Integer riskScore;

    private String ipAddress;

    private String userAgent;

    private String deviceFingerprint;

    private String location;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ==================== 冗余字段 ====================

    @TableField("real_name")
    private String realName;

    @TableField("dept_name")
    private String deptName;

    /**
     * 操作类型枚举
     */
    @Getter
    public enum OperationType {
        EXPORT("EXPORT", "数据导出"),
        BULK_UPDATE("BULK_UPDATE", "批量更新"),
        BULK_DELETE("BULK_DELETE", "批量删除"),
        DATA_DOWNLOAD("DATA_DOWNLOAD", "数据下载"),
        PERMISSION_CHANGE("PERMISSION_CHANGE", "权限变更");

        private final String code;
        private final String desc;

        OperationType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }

    /**
     * 敏感数据类型枚举
     */
    @Getter
    public enum SensitiveDataType {
        PERSONAL_INFO("PERSONAL_INFO", "个人信息"),
        FINANCIAL("FINANCIAL", "财务信息"),
        MEDICAL("MEDICAL", "医疗信息"),
        SECRET("SECRET", "机密信息");

        private final String code;
        private final String desc;

        SensitiveDataType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}
