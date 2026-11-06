package com.cognora.platform.system.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 平台/租户配置表。
 *
 * <p>{@code tenant_id} 可空：{@code NULL} 表示平台级默认值，非空表示该租户的覆盖值。
 * 读取时以租户覆盖优先合并；平台级写入仅平台超级管理员可执行。</p>
 *
 * @author Deng
 * @since 2026-11-06
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_config")
public class SysConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private UUID id;

    /**
     * 平台级为 null；租户覆盖为该租户 ID。
     *
     * <p>刻意不加 {@code FieldFill.INSERT}：元数据处理器会把租户上下文写进来，
     * 平台级行需要保持 {@code NULL}，因此由服务层显式赋值。</p>
     */
    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("config_key")
    private String configKey;

    @TableField("config_value")
    private String configValue;

    @TableField("value_type")
    private String valueType;

    @TableField("description")
    private String description;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private UUID createBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private UUID updateBy;

    @TableLogic(value = "false", delval = "true")
    @TableField("deleted")
    private Boolean deleted;
}
