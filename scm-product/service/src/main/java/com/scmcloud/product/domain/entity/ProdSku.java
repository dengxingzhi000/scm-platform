package com.scmcloud.product.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * SKU 搴撳瓨鍗曚綅锟?
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("prod_sku")
public class ProdSku {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;


    @TableField("spu_id")
    private String spuId;


    @TableField("sku_code")
    private String skuCode;


    @TableField("sku_name")
    private String skuName;

    @TableField("attributes")
    private String attributes;

    @TableField("original_price")
    private BigDecimal originalPrice;


    @TableField("selling_price")
    private BigDecimal sellingPrice;


    @TableField("cost_price")
    private BigDecimal costPrice;

    @TableField("stock")
    private Integer stock;

    @TableField("available_stock")
    private Integer availableStock;

    @TableField("locked_stock")
    private Integer lockedStock;

    @TableField("sales_count")
    private Integer salesCount;

    @TableField("weight")
    private BigDecimal weight;

    @TableField("volume")
    private BigDecimal volume;

    @TableField("barcode")
    private String barcode;

    @TableField("image_url")
    private String imageUrl;

    @TableField("images")
    private String images;

    @TableField("status")
    private Integer status;


    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("deleted")
    private Boolean deleted;

    @TableField("remark")
    private String remark;

}
