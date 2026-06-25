package com.scmcloud.search.service;

import com.scmcloud.search.domain.ProductDocument;

import java.util.List;

public interface IProductSearchService {

    List<ProductDocument> searchProducts(String keyword, Long categoryId, Long brandId, int page, int size);

    List<ProductDocument> getSimilarProducts(Long productId, int limit);

    List<ProductDocument> getPersonalizedResults(Long userId, int limit);

    void indexProduct(ProductDocument product);

    void removeProduct(Long productId);

    void reindexAll();
}
