package com.scmcloud.search.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ProductSearchItem implements Serializable {

    private String id;
    private Long productId;
    private Long sellerId;
    private String title;
    private String subtitle;
    private String description;
    private Long categoryId;
    private Long brandId;
    private String brandName;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer salesCount;
    private BigDecimal rating;
    private Integer reviewCount;
    private String mainImage;
    private String status;
}
