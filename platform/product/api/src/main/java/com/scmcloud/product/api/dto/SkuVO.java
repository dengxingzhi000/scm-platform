package com.scmcloud.product.api.dto;

import java.math.BigDecimal;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SkuVO {

    private Long id;
    private Long productId;
    private String skuCode;
    private String spec;
    private BigDecimal price;
    private Integer status;
}
