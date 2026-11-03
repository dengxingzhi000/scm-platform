package com.scmcloud.product.api.dto;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ProductSearchResult {

    private List<ProductVO> items;
    private long total;
    private int page;
    private int size;
}
