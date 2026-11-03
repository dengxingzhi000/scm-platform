package com.cognora.platform.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.domain.PageResult;
import com.cognora.platform.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.cognora.platform.product.domain.entity.ProdSku;
import com.cognora.platform.product.service.IProdSkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/prod-sku")
@RequiredArgsConstructor
@Tag(name = "SKU 管理", description = "商品SKU的查询、按SPU获取与增删改接口。")
public class ProdSkuController {
    private final IProdSkuService skuService;

    @Operation(summary = "分页查询 SKU", description = "分页查询未删除且未标记删除状态的SKU，可按所属SPU筛选。")
    @GetMapping
    public ApiResponse<PageResult<ProdSku>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "所属SPU ID，可选，用于筛选该SPU下的SKU")
            @RequestParam(required = false) String spuId) {

        Page<ProdSku> result = skuService.pageBySpuId(spuId, page, size);
        return ApiResponse.success(PageResult.of(result));
    }

    @Operation(summary = "查询 SKU 详情", description = "根据SKU ID查询SKU的详细信息。")
    @GetMapping("/{id}")
    public ApiResponse<ProdSku> getById(@Parameter(description = "SKU ID") @PathVariable String id) {
        ProdSku sku = skuService.getById(id);
        return ApiResponse.success(sku);
    }

    @Operation(summary = "按 SPU 查询 SKU", description = "查询指定SPU下的全部SKU列表。")
    @GetMapping("/spu/{spuId}")
    public ApiResponse<List<ProdSku>> listBySpuId(@Parameter(description = "SPU ID") @PathVariable String spuId) {
        List<ProdSku> result = skuService.listBySpuId(spuId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "创建 SKU", description = "新增SKU，自动生成ID与时间字段，未指定状态时默认为启用。")
    @PostMapping
    public ApiResponse<Void> create(@RequestBody ProdSku sku) {
        skuService.createSku(sku);
        return ApiResponse.success();
    }

    @Operation(summary = "更新 SKU", description = "根据SKU ID更新SKU信息并刷新更新时间。")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@Parameter(description = "SKU ID") @PathVariable String id, @RequestBody ProdSku sku) {
        skuService.updateSku(id, sku);
        return ApiResponse.success();
    }

    @Operation(summary = "删除 SKU", description = "根据SKU ID逻辑删除SKU并将其状态置为删除。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "SKU ID") @PathVariable String id) {
        skuService.deleteSku(id);
        return ApiResponse.success();
    }
}
