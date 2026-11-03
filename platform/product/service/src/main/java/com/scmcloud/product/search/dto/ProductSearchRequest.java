package com.scmcloud.product.search.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 鍟嗗搧鎼滅储璇锋眰 DTO
 *
 * <p>鏀寔澶氭潯浠剁粍鍚堟悳锟?
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
public class ProductSearchRequest {

    private String keyword;

    private String categoryId;

    private String brandId;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private String sortBy;

    private String sortOrder;

    private Integer page = 1;

    private Integer size = 20;
}