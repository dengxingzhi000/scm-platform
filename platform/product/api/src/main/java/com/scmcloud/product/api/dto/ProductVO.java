package com.scmcloud.product.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ProductVO {

    private Long id;
    private String name;
    private String code;
    private String category;
    private String brand;
    private String unit;
    private Integer status;
}
