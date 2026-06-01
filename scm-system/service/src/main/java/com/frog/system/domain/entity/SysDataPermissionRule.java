package com.frog.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.frog.common.mybatisPlus.handler.StringArrayTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 数据权限规则�?
 *
 * @author Deng
 * @since 2025-12-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "sys_data_permission_rule", autoResultMap = true)
public class SysDataPermissionRule {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("tenant_id")
    private UUID tenantId;

    private String ruleName;

    private String ruleCode;

    private String resourceType;

    private Integer ruleType;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> ruleConfig;

    private String sqlCondition;

    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] visibleFields;

    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] editableFields;

    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] maskedFields;

    private Integer priority;

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
    private Boolean deleted;

    /**
     * 规则类型枚举
     */
    @Getter
    public enum RuleType {
        ALL(1, "全部数据"),
        CUSTOM_SQL(2, "自定义SQL"),
        DEPT(3, "本部门"),
        DEPT_AND_CHILDREN(4, "本部门及以下"),
        SELF(5, "仅本人");

        private final int code;
        private final String desc;

        RuleType(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}
