package scm.product.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品搜索响应 DTO
 *
 * <p>返回给前端的商品搜索结果
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
@Schema(description = "商品搜索响应")
public class ProductSearchResponse {

    @Schema(description = "SPU ID", example = "spu_001")
    private String id;

    @Schema(description = "SPU 编码", example = "SPU20251226001")
    private String spuCode;

    @Schema(description = "SPU 名称", example = "iPhone 15 Pro Max")
    private String spuName;

    @Schema(description = "分类 ID", example = "cat_001")
    private String categoryId;

    @Schema(description = "分类名称", example = "手机数码")
    private String categoryName;

    @Schema(description = "品牌 ID", example = "brand_001")
    private String brandId;

    @Schema(description = "品牌名称", example = "Apple")
    private String brandName;

    @Schema(description = "商品描述", example = "全新钛金属设计，A17 Pro芯片...")
    private String description;

    @Schema(description = "主图 URL", example = "https://cdn.example.com/iphone15pro.jpg")
    private String mainImage;

    @Schema(description = "最低价格", example = "9999.00")
    private BigDecimal minPrice;

    @Schema(description = "最高价格", example = "13999.00")
    private BigDecimal maxPrice;

    @Schema(description = "总库存", example = "1000")
    private Integer totalStock;

    @Schema(description = "总销量", example = "5000")
    private Integer totalSales;

    @Schema(description = "排序权重", example = "100")
    private Integer sortOrder;

    @Schema(description = "状态: 0-草稿, 1-上架, 2-下架, 3-删除", example = "1")
    private Integer status;

    @Schema(description = "发布时间", example = "2025-12-01T10:00:00")
    private LocalDateTime publishedAt;

    @Schema(description = "创建时间", example = "2025-12-01T10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2025-12-26T10:00:00")
    private LocalDateTime updateTime;
}