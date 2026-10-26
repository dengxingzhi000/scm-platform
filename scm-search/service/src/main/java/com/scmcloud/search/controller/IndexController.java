package com.scmcloud.search.controller;

import com.scmcloud.search.domain.ProductDocument;
import com.scmcloud.search.service.IProductIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "搜索索引", description = "提供商品搜索索引的批量写入、全量重建与单条删除接口。")
public class IndexController {

    private final IProductIndexService indexService;

    @Operation(summary = "批量索引商品", description = "将一批商品文档批量写入搜索索引并返回成功条数。")
    @PostMapping("/products")
    public int bulkIndex(@RequestBody List<ProductDocument> documents) {
        log.info("[API] Bulk index {} products", documents == null ? 0 : documents.size());
        return indexService.bulkIndex(documents);
    }

    @Operation(summary = "全量重建索引", description = "重新构建全部商品的搜索索引并返回索引商品数量。")
    @PostMapping("/reindex")
    public int reindexAll() {
        log.info("[API] Reindex all products");
        return indexService.reindexAll();
    }

    @Operation(summary = "删除商品索引", description = "根据商品 ID 从搜索索引中删除对应商品文档。")
    @DeleteMapping("/products/{productId}")
    public boolean deleteProduct(@Parameter(description = "商品ID") @PathVariable Long productId) {
        indexService.deleteProduct(productId);
        return true;
    }
}
