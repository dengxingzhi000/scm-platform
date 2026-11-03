package com.scmcloud.product.search.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import com.scmcloud.product.search.document.ProductDocument;

import java.math.BigDecimal;

/**
 * 鍟嗗搧鎼滅储 Repository
 *
 * <p>鍩轰簬 Spring Data Elasticsearch 鐨勫晢鍝佹悳绱㈡帴锟?
 *
 * <p>鏀寔鍔熻兘锟?
 * - 鍏ㄦ枃鎼滅储锛坰puName, description, seoKeywords锟?
 * - 鍒嗙被杩囨护
 * - 鍝佺墝杩囨护
 * - 浠锋牸鍖洪棿杩囨护
 * - 鐘舵€佽繃婊わ紙浠呰繑鍥炰笂鏋跺晢鍝侊級
 * - 澶氱鎺掑簭锛堥攢閲忋€佷环鏍笺€佸彂甯冩椂闂淬€佹洿鏂版椂闂达級
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    /**
     * 锟絊PU 鍚嶇О鎼滅储锛堜笂鏋跺晢鍝侊級
     *
     * @param spuName  SPU 鍚嶇О锛堟敮鎸佹ā绯婂尮閰嶏級
     * @param status   鍟嗗搧鐘舵€侊紙1-涓婃灦锟?
     * @param pageable 鍒嗛〉鍙傛暟
     * @return 鍟嗗搧鍒楄〃
     */
    Page<ProductDocument> findBySpuNameAndStatus(String spuName, Integer status, Pageable pageable);

    /**
     * 鎸夊垎绫绘悳绱紙涓婃灦鍟嗗搧锟?
     *
     * @param categoryId 鍒嗙被 ID
     * @param status     鍟嗗搧鐘舵€侊紙1-涓婃灦锟?
     * @param pageable   鍒嗛〉鍙傛暟
     * @return 鍟嗗搧鍒楄〃
     */
    Page<ProductDocument> findByCategoryIdAndStatus(String categoryId, Integer status, Pageable pageable);

    /**
     * 鎸夊搧鐗屾悳绱紙涓婃灦鍟嗗搧锟?
     *
     * @param brandId  鍝佺墝 ID
     * @param status   鍟嗗搧鐘舵€侊紙1-涓婃灦锟?
     * @param pageable 鍒嗛〉鍙傛暟
     * @return 鍟嗗搧鍒楄〃
     */
    Page<ProductDocument> findByBrandIdAndStatus(String brandId, Integer status, Pageable pageable);

    /**
     * 鎸変环鏍煎尯闂存悳绱紙涓婃灦鍟嗗搧锟?
     *
     * @param minPrice 鏈€浣庝环锟?
     * @param maxPrice 鏈€楂樹环锟?
     * @param status   鍟嗗搧鐘舵€侊紙1-涓婃灦锟?
     * @param pageable 鍒嗛〉鍙傛暟
     * @return 鍟嗗搧鍒楄〃
     */
    Page<ProductDocument> findByMinPriceGreaterThanEqualAndMaxPriceLessThanEqualAndStatus(
            BigDecimal minPrice, BigDecimal maxPrice, Integer status, Pageable pageable);

    /**
     * 鍏ㄦ枃鎼滅储锛坰puName + description + seoKeywords锟?
     *
     * <p>浣跨敤 Elasticsearch Query DSL 杩涜澶氬瓧娈垫悳锟?
     *
     * @param keyword  鎼滅储鍏抽敭锟?
     * @param status   鍟嗗搧鐘舵€侊紙1-涓婃灦锟?
     * @param pageable 鍒嗛〉鍙傛暟
     * @return 鍟嗗搧鍒楄〃
     */
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"spuName^3\", \"description^2\", " +
            "\"seoKeywords\"], \"type\": \"best_fields\"}}], \"filter\": [{\"term\": {\"status\": \"?1\"}}]}}")
    Page<ProductDocument> fullTextSearch(String keyword, Integer status, Pageable pageable);

    /**
     * 楂樼骇鎼滅储锛堟敮鎸佸垎绫汇€佸搧鐗屻€佷环鏍煎尯闂淬€佸叧閿瘝缁勫悎杩囨护锟?
     *
     * @param keyword    鎼滅储鍏抽敭璇嶏紙鍙€夛級
     * @param categoryId 鍒嗙被 ID锛堝彲閫夛級
     * @param brandId    鍝佺墝 ID锛堝彲閫夛級
     * @param minPrice   鏈€浣庝环鏍硷紙鍙€夛級
     * @param maxPrice   鏈€楂樹环鏍硷紙鍙€夛級
     * @param status     鍟嗗搧鐘舵€侊紙1-涓婃灦锟?
     * @param pageable   鍒嗛〉鍙傛暟
     * @return 鍟嗗搧鍒楄〃
     */
    @Query("""
            {
              "bool": {
                "must": [
                  {"multi_match": {"query": "?0", "fields": ["spuName^3", "description^2", "seoKeywords"], "type": "best_fields"}}
                ],
                "filter": [
                  {"term": {"categoryId": "?1"}},
                  {"term": {"brandId": "?2"}},
                  {"range": {"minPrice": {"gte": "?3"}}},
                  {"range": {"maxPrice": {"lte": "?4"}}},
                  {"term": {"status": "?5"}}
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
            Integer status,
            Pageable pageable
    );

    /**
     * 鐑棬鍟嗗搧锛堟寜閿€閲忔帓搴忥級
     *
     * @param status   鍟嗗搧鐘舵€侊紙1-涓婃灦锟?
     * @param pageable 鍒嗛〉鍙傛暟
     * @return 鍟嗗搧鍒楄〃
     */
    Page<ProductDocument> findByStatusOrderByTotalSalesDesc(Integer status, Pageable pageable);

    /**
     * 鏈€鏂板晢鍝侊紙鎸夊彂甯冩椂闂存帓搴忥級
     *
     * @param status   鍟嗗搧鐘舵€侊紙1-涓婃灦锟?
     * @param pageable 鍒嗛〉鍙傛暟
     * @return 鍟嗗搧鍒楄〃
     */
    Page<ProductDocument> findByStatusOrderByPublishedAtDesc(Integer status, Pageable pageable);
}