package com.scmcloud.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.domain.PageResult;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.product.domain.entity.ProdBrand;
import com.scmcloud.product.service.IProdBrandService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/prod-brand")
@RequiredArgsConstructor
public class ProdBrandController {
    private final IProdBrandService brandService;

    @GetMapping
    public ApiResponse<PageResult<ProdBrand>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {

        Page<ProdBrand> result = brandService.lambdaQuery()
                .like(name != null, ProdBrand::getBrandName, name)
                .eq(ProdBrand::getDeleted, false)
                .orderByAsc(ProdBrand::getSortOrder)
                .page(new Page<>(page, size));

        return ApiResponse.success(PageResult.of(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProdBrand> getById(@PathVariable String id) {
        ProdBrand brand = brandService.getById(id);
        return ApiResponse.success(brand);
    }

    @GetMapping("/search")
    public ApiResponse<List<ProdBrand>> search(
            @RequestParam(required = false) String name) {
        List<ProdBrand> result = brandService.searchByName(name);
        return ApiResponse.success(result);
    }

    @GetMapping("/featured")
    public ApiResponse<List<ProdBrand>> listFeatured() {
        List<ProdBrand> result = brandService.listFeatured();
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody ProdBrand brand) {
        brand.setId(UUID.randomUUID().toString());
        brand.setCreateTime(LocalDateTime.now());
        brand.setUpdateTime(LocalDateTime.now());
        brand.setDeleted(false);
        brandService.save(brand);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable String id, @RequestBody ProdBrand brand) {
        brand.setId(id);
        brand.setUpdateTime(LocalDateTime.now());
        brandService.updateById(brand);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        brandService.lambdaUpdate()
                .eq(ProdBrand::getId, id)
                .set(ProdBrand::getDeleted, true)
                .set(ProdBrand::getUpdateTime, LocalDateTime.now())
                .update();
        return ApiResponse.success();
    }
}
