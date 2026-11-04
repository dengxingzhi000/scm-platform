package com.cognora.platform.product.search.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import com.cognora.platform.product.search.document.ProductDocument;

import java.math.BigDecimal;

/**
 * 鍟嗗搧鎼滅储 Repository
 *
 * <p>所有查询都带 tenantId 条件；不提供无租户维度的 finder，避免跨租户读取。
 */
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    Page<ProductDocument> findByTenantIdAndSpuNameAndStatus(
            String tenantId, String spuName, Integer status, Pageable pageable);

    Page<ProductDocument> findByTenantIdAndCategoryIdAndStatus(
            String tenantId, String categoryId, Integer status, Pageable pageable);

    Page<ProductDocument> findByTenantIdAndBrandIdAndStatus(
            String tenantId, String brandId, Integer status, Pageable pageable);

    Page<ProductDocument> findByTenantIdAndMinPriceGreaterThanEqualAndMaxPriceLessThanEqualAndStatus(
            String tenantId, BigDecimal minPrice, BigDecimal maxPrice, Integer status, Pageable pageable);

    /**
     * 全文搜索（spuName + description + seoKeywords），强制租户过滤。
     */
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"spuName^3\", \"description^2\", " +
            "\"seoKeywords\"], \"type\": \"best_fields\"}}], \"filter\": [{\"term\": {\"tenant_id\": \"?1\"}}, " +
            "{\"term\": {\"status\": \"?2\"}}]}}")
    Page<ProductDocument> fullTextSearch(String keyword, String tenantId, Integer status, Pageable pageable);

    /**
     * 高级搜索（关键词 + 分类 + 品牌 + 价格区间），强制租户过滤。
     */
    @Query("""
            {
              "bool": {
                "must": [
                  {"multi_match": {"query": "?0", "fields": ["spuName^3", "description^2", "seoKeywords"], "type": "best_fields"}}
                ],
                "filter": [
                  {"term": {"tenant_id": "?5"}},
                  {"term": {"categoryId": "?1"}},
                  {"term": {"brandId": "?2"}},
                  {"range": {"minPrice": {"gte": "?3"}}},
                  {"range": {"maxPrice": {"lte": "?4"}}},
                  {"term": {"status": "?6"}}
                ]
              }
            }
            """)
    Page<ProductDocument> advancedSearch(
            String keyword,
            String categoryId,
            String brandId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String tenantId,
            Integer status,
            Pageable pageable
    );

    Page<ProductDocument> findByTenantIdAndStatusOrderByTotalSalesDesc(
            String tenantId, Integer status, Pageable pageable);

    Page<ProductDocument> findByTenantIdAndStatusOrderByPublishedAtDesc(
            String tenantId, Integer status, Pageable pageable);

    long deleteByTenantIdAndId(String tenantId, String id);
}
