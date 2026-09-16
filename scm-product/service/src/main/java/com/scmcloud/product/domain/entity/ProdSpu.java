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
 * SPU 鏍囧噯浜у搧鍗曞厓锟?
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("prod_spu")
public class ProdSpu {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;


    @TableField("spu_code")
    private String spuCode;


    @TableField("spu_name")
    private String spuName;

    @TableField("category_id")
    private String categoryId;


    @TableField("brand_id")
    private String brandId;

    @TableField("description")
    private String description;

    @TableField("detail_html")
    private String detailHtml;

    @TableField("images")
    private String images;

    @TableField("main_image")
    private String mainImage;

    @TableField("video_url")
    private String videoUrl;

    @TableField("min_price")
    private BigDecimal minPrice;

    @TableField("max_price")
    private BigDecimal maxPrice;

    @TableField("total_stock")
    private Integer totalStock;

    @TableField("total_sales")
    private Integer totalSales;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("seo_title")
    private String seoTitle;

    @TableField("seo_keywords")
    private String seoKeywords;

    @TableField("seo_description")
    private String seoDescription;

    @TableField("status")
    private Integer status;


    @TableField("published_at")
    private LocalDateTime publishedAt;

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
