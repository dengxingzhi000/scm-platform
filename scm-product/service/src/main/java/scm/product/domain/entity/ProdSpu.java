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
 * SPU 标准产品单元表
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("prod_spu")
@Schema(description = "SPU 标准产品单元表")
public class ProdSpu implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    @Schema(description = "SPU ID")
    private String id;


    @TableField("spu_code")
    @Schema(description = "SPU 编码")
    private String spuCode;


    @TableField("spu_name")
    @Schema(description = "SPU 名称")
    private String spuName;

    @TableField("category_id")
    @Schema(description = "分类 ID")
    private String categoryId;


    @TableField("brand_id")
    @Schema(description = "品牌 ID")
    private String brandId;

    @TableField("description")
    @Schema(description = "商品描述")
    private String description;

    @TableField("detail_html")
    @Schema(description = "商品详情 HTML")
    private String detailHtml;

    @TableField("images")
    @Schema(description = "JSON数组，存储图片URL列表")
    private String images;

    @TableField("main_image")
    @Schema(description = "主图 URL")
    private String mainImage;

    @TableField("video_url")
    @Schema(description = "视频 URL")
    private String videoUrl;

    @TableField("min_price")
    @Schema(description = "最低价格")
    private BigDecimal minPrice;

    @TableField("max_price")
    @Schema(description = "最高价格")
    private BigDecimal maxPrice;

    @TableField("total_stock")
    @Schema(description = "总库存")
    private Integer totalStock;

    @TableField("total_sales")
    @Schema(description = "总销量")
    private Integer totalSales;

    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;

    @TableField("seo_title")
    @Schema(description = "SEO 标题")
    private String seoTitle;

    @TableField("seo_keywords")
    @Schema(description = "SEO 关键词")
    private String seoKeywords;

    @TableField("seo_description")
    @Schema(description = "SEO 描述")
    private String seoDescription;

    @TableField("status")
    @Schema(description = "状态:0-草稿,1-上架,2-下架,3-删除")
    private Integer status;


    @TableField("published_at")
    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

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
