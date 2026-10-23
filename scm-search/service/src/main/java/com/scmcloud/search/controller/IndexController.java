package com.scmcloud.search.controller;

import com.scmcloud.search.domain.ProductDocument;
import com.scmcloud.search.service.IProductIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/search/index")
@RequiredArgsConstructor
public class IndexController {

    private final IProductIndexService indexService;

    @PostMapping("/products")
    public int bulkIndex(@RequestBody List<ProductDocument> documents) {
        log.info("[API] Bulk index {} products", documents == null ? 0 : documents.size());
        return indexService.bulkIndex(documents);
    }

    @PostMapping("/reindex")
    public int reindexAll() {
        log.info("[API] Reindex all products");
        return indexService.reindexAll();
    }

    @DeleteMapping("/products/{productId}")
    public boolean deleteProduct(@PathVariable Long productId) {
        indexService.deleteProduct(productId);
        return true;
    }
}
