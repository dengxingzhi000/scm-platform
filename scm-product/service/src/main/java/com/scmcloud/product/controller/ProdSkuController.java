package com.scmcloud.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.domain.PageResult;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.product.domain.entity.ProdSku;
import com.scmcloud.product.service.IProdSkuService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/prod-sku")
@RequiredArgsConstructor
public class ProdSkuController {
    private final IProdSkuService skuService;

    @GetMapping
    public ApiResponse<PageResult<ProdSku>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String spuId) {

        Page<ProdSku> result = skuService.lambdaQuery()
                .eq(spuId != null, ProdSku::getSpuId, spuId)
                .eq(ProdSku::getDeleted, false)
                .ne(ProdSku::getStatus, 3)
                .orderByDesc(ProdSku::getCreateTime)
                .page(new Page<>(page, size));

        return ApiResponse.success(PageResult.of(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProdSku> getById(@PathVariable String id) {
        ProdSku sku = skuService.getById(id);
        return ApiResponse.success(sku);
    }

    @GetMapping("/spu/{spuId}")
    public ApiResponse<List<ProdSku>> listBySpuId(@PathVariable String spuId) {
        List<ProdSku> result = skuService.listBySpuId(spuId);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody ProdSku sku) {
        sku.setId(UUID.randomUUID().toString());
        sku.setCreateTime(LocalDateTime.now());
        sku.setUpdateTime(LocalDateTime.now());
        sku.setDeleted(false);
        if (sku.getStatus() == null) {
            sku.setStatus(1);
        }
        skuService.save(sku);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable String id, @RequestBody ProdSku sku) {
        sku.setId(id);
        sku.setUpdateTime(LocalDateTime.now());
        skuService.updateById(sku);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        skuService.lambdaUpdate()
                .eq(ProdSku::getId, id)
                .set(ProdSku::getDeleted, true)
                .set(ProdSku::getStatus, 3)
                .set(ProdSku::getUpdateTime, LocalDateTime.now())
                .update();
        return ApiResponse.success();
    }
}
