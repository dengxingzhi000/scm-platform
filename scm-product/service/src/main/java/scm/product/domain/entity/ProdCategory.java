package scm.product.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serial;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 商品分类表
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("prod_category")
@Schema(description = "商品分类表")
public class ProdCategory implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    @Schema(description = "分类 ID")
    private String id;


    @TableField("category_code")
    @Schema(description = "分类编码")
    private String categoryCode;


    @TableField("category_name")
    @Schema(description = "分类名称")
    private String categoryName;

    @TableField("parent_id")
    @Schema(description = "父分类 ID")
    private String parentId;

    @TableField("level")
    @Schema(description = "分类层级")
    private Integer level;


    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;

    @TableField("icon_url")
    @Schema(description = "分类图标 URL")
    private String iconUrl;

    @TableField("image_url")
    @Schema(description = "分类图片 URL")
    private String imageUrl;

    @TableField("description")
    @Schema(description = "分类描述")
    private String description;

    @TableField("is_leaf")
    @Schema(description = "是否叶子分类（只有叶子分类可以挂商品）")
    private Boolean isLeaf;

    @TableField("enabled")
    @Schema(description = "是否启用")
    private Boolean enabled;

    @TableField("seo_title")
    @Schema(description = "SEO 标题")
    private String seoTitle;

    @TableField("seo_keywords")
    @Schema(description = "SEO 关键词")
    private String seoKeywords;

    @TableField("seo_description")
    @Schema(description = "SEO 描述")
    private String seoDescription;

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
