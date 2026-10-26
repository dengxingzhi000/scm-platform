package com.scmcloud.search.controller;

import com.scmcloud.search.domain.ProductDocument;
import com.scmcloud.search.service.IProductSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "商品搜索", description = "提供商品关键词搜索、相似推荐、个性化推荐与索引维护接口。")
public class SearchController {

    private final IProductSearchService searchService;

    @Operation(summary = "搜索商品", description = "按关键词、类目和品牌分页搜索商品。")
    @GetMapping("/products")
    public List<ProductDocument> searchProducts(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "类目ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "品牌ID") @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("[API] Search products: keyword={}, categoryId={}, brandId={}", keyword, categoryId, brandId);
        return searchService.searchProducts(keyword, categoryId, brandId, page, size);
    }

    @Operation(summary = "查询相似商品", description = "根据商品 ID 返回内容相似的推荐商品列表。")
    @GetMapping("/products/{productId}/similar")
    public List<ProductDocument> getSimilarProducts(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "返回结果数量") @RequestParam(defaultValue = "10") int limit) {
        log.info("[API] Get similar products: productId={}, limit={}", productId, limit);
        return searchService.getSimilarProducts(productId, limit);
    }

    @Operation(summary = "查询个性化推荐", description = "根据用户 ID 返回个性化的商品推荐结果。")
    @GetMapping("/products/personalized/{userId}")
    public List<ProductDocument> getPersonalizedResults(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "返回结果数量") @RequestParam(defaultValue = "20") int limit) {
        log.info("[API] Get personalized results: userId={}, limit={}", userId, limit);
        return searchService.getPersonalizedResults(userId, limit);
    }

    @Operation(summary = "索引单个商品", description = "将单个商品文档写入搜索索引。")
    @PostMapping("/index/product")
    public boolean indexProduct(@RequestBody ProductDocument product) {
        log.info("[API] Index product: productId={}", product.getProductId());
        searchService.indexProduct(product);
        return true;
    }

    @Operation(summary = "移除商品索引", description = "根据商品 ID 从搜索索引中移除对应商品。")
    @DeleteMapping("/index/product/{productId}")
    public boolean removeProduct(@Parameter(description = "商品ID") @PathVariable Long productId) {
        log.info("[API] Remove product from index: productId={}", productId);
        searchService.removeProduct(productId);
        return true;
    }
}
