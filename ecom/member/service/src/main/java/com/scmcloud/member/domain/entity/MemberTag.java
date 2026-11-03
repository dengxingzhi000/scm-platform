package com.scmcloud.member.domain.entity;

import com.scmcloud.common.domain.TenantId;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("mem_member_tag")
public class MemberTag {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private TenantId tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("tag_name")
    private String tagName;

    @TableField("tag_type")
    private String tagType;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
