package com.cognora.ecom.search.repository;

import com.cognora.ecom.search.domain.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductDocumentRepository extends ElasticsearchRepository<ProductDocument, String> {

    List<ProductDocument> findByProductId(Long productId);

    List<ProductDocument> findByCategoryId(Long categoryId);

    List<ProductDocument> findByBrandId(Long brandId);

    List<ProductDocument> findByStatus(String status);
}
