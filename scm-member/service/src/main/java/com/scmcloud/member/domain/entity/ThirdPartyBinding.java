package com.scmcloud.member.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("mem_third_party_binding")
public class ThirdPartyBinding {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("platform")
    private String platform;

    @TableField("open_id")
    private String openId;

    @TableField("union_id")
    private String unionId;

    @TableField("nickname")
    private String nickname;

    @TableField("avatar")
    private String avatar;

    @TableField("binding_time")
    private LocalDateTime bindingTime;

    @TableField("status")
    private Integer status;
}
