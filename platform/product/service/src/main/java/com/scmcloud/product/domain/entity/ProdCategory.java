package com.scmcloud.product.domain.entity;

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
 * 鍟嗗搧鍒嗙被锟?
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("prod_category")
public class ProdCategory {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;


    @TableField("category_code")
    private String categoryCode;


    @TableField("category_name")
    private String categoryName;

    @TableField("parent_id")
    private String parentId;

    @TableField("level")
    private Integer level;


    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("icon_url")
    private String iconUrl;

    @TableField("image_url")
    private String imageUrl;

    @TableField("description")
    private String description;

    @TableField("is_leaf")
    private Boolean isLeaf;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("seo_title")
    private String seoTitle;

    @TableField("seo_keywords")
    private String seoKeywords;

    @TableField("seo_description")
    private String seoDescription;

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
