package com.scmcloud.promotion.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.scmcloud.common.domain.TenantId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pro_group_buy_member")
public class GroupBuyMember {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private TenantId tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("group_buy_id")
    private Long groupBuyId;

    @TableField("user_id")
    private String userId;

    @TableField("order_no")
    private String orderNo;

    @TableField("status")
    private Integer status;

    @TableField("joined_at")
    private LocalDateTime joinedAt;
}
