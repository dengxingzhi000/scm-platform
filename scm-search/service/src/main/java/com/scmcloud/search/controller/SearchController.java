package com.scmcloud.search.controller;

import com.scmcloud.search.domain.ProductDocument;
import com.scmcloud.search.service.IProductSearchService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final IProductSearchService searchService;

    @GetMapping("/products")
    public List<ProductDocument> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("[API] Search products: keyword={}, categoryId={}, brandId={}", keyword, categoryId, brandId);
        return searchService.searchProducts(keyword, categoryId, brandId, page, size);
    }

    @GetMapping("/products/{productId}/similar")
    public List<ProductDocument> getSimilarProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("[API] Get similar products: productId={}, limit={}", productId, limit);
        return searchService.getSimilarProducts(productId, limit);
    }

    @GetMapping("/products/personalized/{userId}")
    public List<ProductDocument> getPersonalizedResults(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        log.info("[API] Get personalized results: userId={}, limit={}", userId, limit);
        return searchService.getPersonalizedResults(userId, limit);
    }

    @PostMapping("/index/product")
    public boolean indexProduct(@RequestBody ProductDocument product) {
        log.info("[API] Index product: productId={}", product.getProductId());
        searchService.indexProduct(product);
        return true;
    }

    @DeleteMapping("/index/product/{productId}")
    public boolean removeProduct(@PathVariable Long productId) {
        log.info("[API] Remove product from index: productId={}", productId);
        searchService.removeProduct(productId);
        return true;
    }
}
