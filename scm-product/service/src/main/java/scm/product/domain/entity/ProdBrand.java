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
 * 商品品牌表
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("prod_brand")
@Schema(description = "商品品牌表")
public class ProdBrand implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    @Schema(description = "品牌 ID")
    private String id;


    @TableField("brand_code")
    @Schema(description = "品牌编码")
    private String brandCode;


    @TableField("brand_name")
    @Schema(description = "品牌名称")
    private String brandName;

    @TableField("brand_name_en")
    @Schema(description = "品牌英文名称")
    private String brandNameEn;

    @TableField("logo_url")
    @Schema(description = "品牌Logo URL")
    private String logoUrl;

    @TableField("description")
    @Schema(description = "品牌描述")
    private String description;

    @TableField("website")
    @Schema(description = "官方网站")
    private String website;

    @TableField("country")
    @Schema(description = "国家")
    private String country;

    @TableField("established_year")
    @Schema(description = "成立年份")
    private Integer establishedYear;

    @TableField("featured")
    @Schema(description = "是否推荐品牌")
    private Boolean featured;

    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;

    @TableField("enabled")
    @Schema(description = "是否启用")
    private Boolean enabled;

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
