package com.scmcloud.member.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("mem_member_level")
public class MemberLevel {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("level_code")
    private String levelCode;

    @TableField("level_name")
    private String levelName;

    @TableField("min_points")
    private Integer minPoints;

    @TableField("discount_rate")
    private BigDecimal discountRate;

    @TableField("privileges_json")
    private String privilegesJson;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
