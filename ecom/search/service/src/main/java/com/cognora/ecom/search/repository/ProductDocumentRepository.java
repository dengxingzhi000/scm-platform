package com.cognora.ecom.search.repository;

import com.cognora.ecom.search.domain.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * All read paths are tenant-scoped on purpose: there is no unscoped finder left, so a
 * caller cannot accidentally query documents that belong to another tenant.
 */
@Repository
public interface ProductDocumentRepository extends ElasticsearchRepository<ProductDocument, String> {

    List<ProductDocument> findByTenantId(String tenantId);

    List<ProductDocument> findByTenantIdAndProductId(String tenantId, Long productId);

    List<ProductDocument> findByTenantIdAndCategoryId(String tenantId, Long categoryId);

    List<ProductDocument> findByTenantIdAndStatus(String tenantId, String status);
}
