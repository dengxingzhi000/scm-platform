package com.scmcloud.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.domain.PageResult;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.product.domain.entity.ProdBrand;
import com.scmcloud.product.service.IProdBrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/prod-brand")
@RequiredArgsConstructor
@Tag(name = "品牌管理", description = "商品品牌的查询、搜索、推荐列表维护与增删改接口。")
public class ProdBrandController {
    private final IProdBrandService brandService;

    @Operation(summary = "分页查询品牌", description = "按品牌名称模糊筛选并分页查询未删除的品牌，结果按排序号升序排列。")
    @GetMapping
    public ApiResponse<PageResult<ProdBrand>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "品牌名称关键字，可选，支持模糊匹配")
            @RequestParam(required = false) String name) {

        Page<ProdBrand> result = brandService.lambdaQuery()
                .like(name != null, ProdBrand::getBrandName, name)
                .eq(ProdBrand::getDeleted, false)
                .orderByAsc(ProdBrand::getSortOrder)
                .page(new Page<>(page, size));

        return ApiResponse.success(PageResult.of(result));
    }

    @Operation(summary = "查询品牌详情", description = "根据品牌ID查询品牌的详细信息。")
    @GetMapping("/{id}")
    public ApiResponse<ProdBrand> getById(@Parameter(description = "品牌ID") @PathVariable String id) {
        ProdBrand brand = brandService.getById(id);
        return ApiResponse.success(brand);
    }

    @Operation(summary = "搜索品牌", description = "根据品牌名称模糊搜索品牌列表。")
    @GetMapping("/search")
    public ApiResponse<List<ProdBrand>> search(
            @Parameter(description = "品牌名称关键字，可选，支持模糊匹配")
            @RequestParam(required = false) String name) {
        List<ProdBrand> result = brandService.searchByName(name);
        return ApiResponse.success(result);
    }

    @Operation(summary = "查询推荐品牌", description = "查询已设置为推荐的全部品牌列表。")
    @GetMapping("/featured")
    public ApiResponse<List<ProdBrand>> listFeatured() {
        List<ProdBrand> result = brandService.listFeatured();
        return ApiResponse.success(result);
    }

    @Operation(summary = "创建品牌", description = "新增品牌，自动生成ID并初始化创建与更新时间。")
    @PostMapping
    public ApiResponse<Void> create(@RequestBody ProdBrand brand) {
        brand.setId(UUID.randomUUID().toString());
        brand.setCreateTime(LocalDateTime.now());
        brand.setUpdateTime(LocalDateTime.now());
        brand.setDeleted(false);
        brandService.save(brand);
        return ApiResponse.success();
    }

    @Operation(summary = "更新品牌", description = "根据品牌ID更新品牌信息并刷新更新时间。")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@Parameter(description = "品牌ID") @PathVariable String id, @RequestBody ProdBrand brand) {
        brand.setId(id);
        brand.setUpdateTime(LocalDateTime.now());
        brandService.updateById(brand);
        return ApiResponse.success();
    }

    @Operation(summary = "删除品牌", description = "根据品牌ID逻辑删除品牌。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "品牌ID") @PathVariable String id) {
        brandService.lambdaUpdate()
                .eq(ProdBrand::getId, id)
                .set(ProdBrand::getDeleted, true)
                .set(ProdBrand::getUpdateTime, LocalDateTime.now())
                .update();
        return ApiResponse.success();
    }
}
