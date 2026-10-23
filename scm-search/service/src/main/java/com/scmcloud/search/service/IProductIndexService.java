package com.scmcloud.search.service;

import com.scmcloud.search.domain.ProductDocument;

import java.util.List;

public interface IProductIndexService {

    int bulkIndex(List<ProductDocument> documents);

    int reindexAll();

    void deleteProduct(Long productId);
}
