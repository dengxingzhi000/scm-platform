package com.scmcloud.mall.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("mall_seller")
public class Seller {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("seller_name")
    private String sellerName;

    @TableField("seller_type")
    private String sellerType;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("license_no")
    private String licenseNo;

    @TableField("license_image")
    private String licenseImage;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
