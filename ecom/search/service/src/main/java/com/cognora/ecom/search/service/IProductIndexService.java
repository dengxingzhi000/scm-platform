package com.cognora.ecom.search.service;

import com.cognora.ecom.search.domain.ProductDocument;

import java.util.List;

public interface IProductIndexService {

    int bulkIndex(List<ProductDocument> documents);

    int reindexAll();

    void deleteProduct(Long productId);
}
