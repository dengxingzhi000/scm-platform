package com.scmcloud.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义数据权限规则实体
 *
 * <p>用于 CUSTOM 数据权限范围，存储用户/角色的自定义访问规则
 *
 * @author Claude Code
 * @since 2025-01-24
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_data_permission_rule")
public class SysDataPermissionRule extends TenantAwareEntity {

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 绑定类型：USER-用户, ROLE-角色
     */
    private String bindType;

    /**
     * 绑定目标 ID（用户 ID 或角色 ID）
     */
    private String bindTargetId;

    /**
     * 资源类型（如：order, product, inventory）
     */
    private String resourceType;

    /**
     * 规则类型：INCLUDE-包含, EXCLUDE-排除
     */
    private String ruleType;

    /**
     * 规则值（JSON 格式，如部门 ID 列表）
     */
    private String ruleValue;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
