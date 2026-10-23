package com.scmcloud.search.api;

import com.scmcloud.search.api.dto.ProductSearchItem;

import java.util.List;

public interface SearchDubboService {

    List<ProductSearchItem> searchProducts(String keyword, Long categoryId, Long brandId, int page, int size);

    void indexProduct(ProductSearchItem item);

    void removeProduct(Long productId);

    int reindexAll();
}
