package com.scmcloud.product.search.dto;

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
public class ProductSearchResponse {

    private String id;

    private String spuCode;

    private String spuName;

    private String categoryId;

    private String categoryName;

    private String brandId;

    private String brandName;

    private String description;

    private String mainImage;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Integer totalStock;

    private Integer totalSales;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime publishedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}