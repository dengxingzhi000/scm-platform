package scm.product.domain.entity;

import java.io.Serial;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * SKU 库存单位表
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("prod_sku")
@Schema(description = "SKU 库存单位表")
public class ProdSku implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    @Schema(description = "SKU ID")
    private String id;


    @TableField("spu_id")
    @Schema(description = "SPU ID")
    private String spuId;


    @TableField("sku_code")
    @Schema(description = "SKU 编码")
    private String skuCode;


    @TableField("sku_name")
    @Schema(description = "SKU 名称")
    private String skuName;

    @TableField("attributes")
    @Schema(description = "SKU属性JSON，如：{\"color\":\"黑色\",\"storage\":\"256GB\"}")
    private String attributes;

    @TableField("original_price")
    @Schema(description = "原价")
    private BigDecimal originalPrice;


    @TableField("selling_price")
    @Schema(description = "售价")
    private BigDecimal sellingPrice;


    @TableField("cost_price")
    @Schema(description = "成本价")
    private BigDecimal costPrice;

    @TableField("stock")
    @Schema(description = "总库存")
    private Integer stock;

    @TableField("available_stock")
    @Schema(description = "可用库存")
    private Integer availableStock;

    @TableField("locked_stock")
    @Schema(description = "锁定库存")
    private Integer lockedStock;

    @TableField("sales_count")
    @Schema(description = "销量")
    private Integer salesCount;

    @TableField("weight")
    @Schema(description = "重量")
    private BigDecimal weight;

    @TableField("volume")
    @Schema(description = "体积")
    private BigDecimal volume;

    @TableField("barcode")
    @Schema(description = "条形码")
    private String barcode;

    @TableField("image_url")
    @Schema(description = "主图URL")
    private String imageUrl;

    @TableField("images")
    @Schema(description = "图片集")
    private String images;

    @TableField("status")
    @Schema(description = "状态:0-停用,1-启用,2-缺货,3-删除")
    private Integer status;


    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField("create_by")
    @Schema(description = "创建人")
    private String createBy;

    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField("update_by")
    @Schema(description = "更新人")
    private String updateBy;

    @TableField("deleted")
    @Schema(description = "是否删除")
    private Boolean deleted;

    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

}
