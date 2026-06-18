package com.scmcloud.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.domain.PageResult;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.product.domain.entity.ProdSpu;
import com.scmcloud.product.service.IProdSpuService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/prod-spu")
@RequiredArgsConstructor
public class ProdSpuController {
    private final IProdSpuService spuService;

    @GetMapping
    public ApiResponse<PageResult<ProdSpu>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) Integer status) {

        Page<ProdSpu> result = spuService.search(keyword, categoryId, brandId, status, page, size);
        return ApiResponse.success(PageResult.of(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProdSpu> getById(@PathVariable String id) {
        ProdSpu spu = spuService.getById(id);
        return ApiResponse.success(spu);
    }

    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<ProdSpu>> listByCategoryId(@PathVariable String categoryId) {
        List<ProdSpu> result = spuService.listByCategoryId(categoryId);
        return ApiResponse.success(result);
    }

    @GetMapping("/brand/{brandId}")
    public ApiResponse<List<ProdSpu>> listByBrandId(@PathVariable String brandId) {
        List<ProdSpu> result = spuService.listByBrandId(brandId);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody ProdSpu spu) {
        spu.setId(UUID.randomUUID().toString());
        spu.setCreateTime(LocalDateTime.now());
        spu.setUpdateTime(LocalDateTime.now());
        spu.setDeleted(false);
        if (spu.getStatus() == null) {
            spu.setStatus(0);
        }
        spuService.save(spu);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable String id, @RequestBody ProdSpu spu) {
        spu.setId(id);
        spu.setUpdateTime(LocalDateTime.now());
        spuService.updateById(spu);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        spuService.lambdaUpdate()
                .eq(ProdSpu::getId, id)
                .set(ProdSpu::getDeleted, true)
                .set(ProdSpu::getStatus, 3)
                .set(ProdSpu::getUpdateTime, LocalDateTime.now())
                .update();
        return ApiResponse.success();
    }
}
