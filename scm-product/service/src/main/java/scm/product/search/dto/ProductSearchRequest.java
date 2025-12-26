package scm.product.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品搜索请求 DTO
 *
 * <p>支持多条件组合搜索
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
@Schema(description = "商品搜索请求")
public class ProductSearchRequest {

    @Schema(description = "搜索关键词（支持商品名称、描述、SEO关键词）", example = "iPhone 15 Pro")
    private String keyword;

    @Schema(description = "分类 ID", example = "cat_001")
    private String categoryId;

    @Schema(description = "品牌 ID", example = "brand_001")
    private String brandId;

    @Schema(description = "最低价格", example = "1000.00")
    private BigDecimal minPrice;

    @Schema(description = "最高价格", example = "10000.00")
    private BigDecimal maxPrice;

    @Schema(description = "排序字段（sales-销量, price-价格, time-发布时间）", example = "sales")
    private String sortBy;

    @Schema(description = "排序方向（asc-升序, desc-降序）", example = "desc")
    private String sortOrder;

    @Schema(description = "页码（从 1 开始）", example = "1")
    private Integer page = 1;

    @Schema(description = "每页数量", example = "20")
    private Integer size = 20;
}