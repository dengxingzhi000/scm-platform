package com.cognora.ecom.search.api;

import com.cognora.ecom.search.api.dto.ProductSearchItem;

import java.util.List;

/**
 * Dubbo callers have no HTTP tenant context, so every method carries the tenant
 * explicitly. The provider rejects a missing or malformed tenant instead of falling
 * back to an unscoped query.
 */
public interface SearchDubboService {

    List<ProductSearchItem> searchProducts(String keyword, Long categoryId, Long brandId,
                                           int page, int size, String tenantId);

    void indexProduct(ProductSearchItem item);

    void removeProduct(Long productId, String tenantId);

    int reindexAll(String tenantId);
}
