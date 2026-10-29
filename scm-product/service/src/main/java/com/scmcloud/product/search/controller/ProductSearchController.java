package com.scmcloud.product.search.controller;

import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.product.search.dto.ProductSearchRequest;
import com.scmcloud.product.search.dto.ProductSearchResponse;
import com.scmcloud.product.search.service.ProductSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 商品搜索 Controller
 *
 * <p>提供商品搜索相关 API
 *
 * <p>功能列表：
 * - 综合搜索（支持关键词、分类、品牌、价格区间、排序）
 * - 热门商品列表
 * - 最新商品列表
 * - 按分类查询商品
 * - 按品牌查询商品
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products/search")
@RequiredArgsConstructor
@Tag(name = "商品搜索", description = "基于搜索引擎的商品综合检索接口，支持热门、最新、分类与品牌维度的查询。")
public class ProductSearchController {
    private final ProductSearchService productSearchService;

    /**
     * 综合搜索
     *
     * <p>支持多条件组合搜索：
     * - 关键词搜索（spuName, description, seoKeywords
     * - 分类筛选
     * - 品牌筛选
     * - 价格区间
     * - 多种排序（销量、价格、时间）
     */
    @Operation(summary = "综合搜索商品", description = "支持关键词、分类、品牌、价格区间与排序条件的商品分页搜索。")
    @PostMapping
    public ApiResponse<Page<ProductSearchResponse>> search(
            @RequestBody ProductSearchRequest request) {

        log.debug("📥 [API] 收到商品搜索请求: {}", (request + "").replace("\r", "_").replace("\n", "_"));

        Page<ProductSearchResponse> result = productSearchService.search(request);

        log.debug("📤 [API] 返回商品搜索结果: 总数={}, 页码={}/{}",
                result.getTotalElements(), result.getNumber() + 1, result.getTotalPages());

        return ApiResponse.success(result);
    }

    /**
     * 热门商品
     *
     * <p>按销量排序的热门商品列表（缓5 分钟
     */
    @Operation(summary = "查询热门商品", description = "按销量倒序分页查询热门商品列表。")
    @GetMapping("/hot")
    public ApiResponse<Page<ProductSearchResponse>> getHotProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        log.debug("📥 [API] 查询热门商品: page={}, size={}", page, size);

        Page<ProductSearchResponse> result = productSearchService.getHotProducts(page, size);

        log.debug("📤 [API] 返回热门商品: 总数={}", result.getTotalElements());

        return ApiResponse.success(result);
    }

    /**
     * 最新商品
     *
     * <p>按发布时间排序的最新商品列表（缓存 5 分钟
     */
    @Operation(summary = "查询最新商品", description = "按发布时间倒序分页查询最新商品列表。")
    @GetMapping("/latest")
    public ApiResponse<Page<ProductSearchResponse>> getLatestProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        log.debug("📥 [API] 查询最新商品 page={}, size={}", page, size);

        Page<ProductSearchResponse> result = productSearchService.getLatestProducts(page, size);

        log.debug("📤 [API] 返回最新商品 总数={}", result.getTotalElements());

        return ApiResponse.success(result);
    }

    /**
     * 按分类查询商品
     *
     * <p>查询指定分类下的所有商品，按销量排
     */
    @Operation(summary = "按分类查询商品", description = "分页查询指定商品分类下的商品，结果按销量排序。")
    @GetMapping("/category/{categoryId}")
    public ApiResponse<Page<ProductSearchResponse>> findByCategory(
            @Parameter(description = "商品分类ID") @PathVariable String categoryId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        log.debug("📥 [API] 按分类查询商品 categoryId={}, page={}, size={}", (categoryId + "").replace("\r", "_").replace("\n", "_"), (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"));

        Page<ProductSearchResponse> result = productSearchService.findByCategory(categoryId, page, size);

        log.debug("📤 [API] 返回分类商品: 总数={}", result.getTotalElements());

        return ApiResponse.success(result);
    }

    /**
     * 按品牌查询商品
     *
     * <p>查询指定品牌下的所有商品，按销量排
     */
    @Operation(summary = "按品牌查询商品", description = "分页查询指定品牌下的商品，结果按销量排序。")
    @GetMapping("/brand/{brandId}")
    public ApiResponse<Page<ProductSearchResponse>> findByBrand(
            @Parameter(description = "品牌ID") @PathVariable String brandId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        log.debug("📥 [API] 按品牌查询商品 brandId={}, page={}, size={}", (brandId + "").replace("\r", "_").replace("\n", "_"), (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"));

        Page<ProductSearchResponse> result = productSearchService.findByBrand(brandId, page, size);

        log.debug("📤 [API] 返回品牌商品: 总数={}", result.getTotalElements());

        return ApiResponse.success(result);
    }

    /**
     * 快速搜索（GET 方式，用于简单场景）
     *
     * <p>适用于简单的关键词搜索，不需要复杂筛
     */
    @Operation(summary = "快速搜索商品", description = "以 GET 方式按关键词快速搜索商品，适用于简单查询场景。")
    @GetMapping
    public ApiResponse<Page<ProductSearchResponse>> quickSearch(
            @Parameter(description = "搜索关键词，可选")
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "排序字段，可选，默认按销量 sales 排序")
            @RequestParam(defaultValue = "sales") String sortBy,
            @Parameter(description = "排序方向，可选，asc 升序或 desc 降序")
            @RequestParam(defaultValue = "desc") String sortOrder) {

        log.debug("📥 [API] 快速搜索 keyword={}, page={}, size={}, sortBy={}, sortOrder={}",
                (keyword + "").replace("\r", "_").replace("\n", "_"), (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (sortBy + "").replace("\r", "_").replace("\n", "_"), (sortOrder + "").replace("\r", "_").replace("\n", "_"));

        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword(keyword);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortOrder(sortOrder);

        Page<ProductSearchResponse> result = productSearchService.search(request);

        log.debug("📤 [API] 返回快速搜索结果 总数={}", result.getTotalElements());

        return ApiResponse.success(result);
    }
}