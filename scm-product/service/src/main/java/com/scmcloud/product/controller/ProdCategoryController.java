package com.scmcloud.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.domain.PageResult;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.product.domain.entity.ProdCategory;
import com.scmcloud.product.service.IProdCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/prod-category")
@RequiredArgsConstructor
@Tag(name = "商品分类", description = "商品分类树的查询、搜索、子分类获取与增删改接口。")
public class ProdCategoryController {
    private final IProdCategoryService categoryService;

    @Operation(summary = "分页查询分类", description = "按分类名称模糊筛选并分页查询未删除的商品分类，结果按排序号升序排列。")
    @GetMapping
    public ApiResponse<PageResult<ProdCategory>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "分类名称关键字，可选，支持模糊匹配")
            @RequestParam(required = false) String name) {

        Page<ProdCategory> result = categoryService.lambdaQuery()
                .like(name != null, ProdCategory::getCategoryName, name)
                .eq(ProdCategory::getDeleted, false)
                .orderByAsc(ProdCategory::getSortOrder)
                .page(new Page<>(page, size));

        return ApiResponse.success(PageResult.of(result));
    }

    @Operation(summary = "查询分类详情", description = "根据分类ID查询商品分类的详细信息。")
    @GetMapping("/{id}")
    public ApiResponse<ProdCategory> getById(@Parameter(description = "分类ID") @PathVariable String id) {
        ProdCategory category = categoryService.getById(id);
        return ApiResponse.success(category);
    }

    @Operation(summary = "查询分类树", description = "查询完整的商品分类树形结构。")
    @GetMapping("/tree")
    public ApiResponse<List<ProdCategory>> getCategoryTree() {
        List<ProdCategory> tree = categoryService.getCategoryTree();
        return ApiResponse.success(tree);
    }

    @Operation(summary = "查询子分类", description = "查询指定父分类下的直接子分类列表。")
    @GetMapping("/children/{parentId}")
    public ApiResponse<List<ProdCategory>> listByParentId(@Parameter(description = "父分类ID") @PathVariable String parentId) {
        List<ProdCategory> children = categoryService.listByParentId(parentId);
        return ApiResponse.success(children);
    }

    @Operation(summary = "搜索分类", description = "根据分类名称模糊搜索商品分类列表。")
    @GetMapping("/search")
    public ApiResponse<List<ProdCategory>> search(
            @Parameter(description = "分类名称关键字，可选，支持模糊匹配")
            @RequestParam(required = false) String name) {
        List<ProdCategory> result = categoryService.searchByName(name);
        return ApiResponse.success(result);
    }

    @Operation(summary = "创建分类", description = "新增商品分类，自动生成ID并初始化创建与更新时间。")
    @PostMapping
    public ApiResponse<Void> create(@RequestBody ProdCategory category) {
        category.setId(UUID.randomUUID().toString());
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        category.setDeleted(false);
        categoryService.save(category);
        return ApiResponse.success();
    }

    @Operation(summary = "更新分类", description = "根据分类ID更新商品分类信息并刷新更新时间。")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@Parameter(description = "分类ID") @PathVariable String id, @RequestBody ProdCategory category) {
        category.setId(id);
        category.setUpdateTime(LocalDateTime.now());
        categoryService.updateById(category);
        return ApiResponse.success();
    }

    @Operation(summary = "删除分类", description = "根据分类ID逻辑删除商品分类。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "分类ID") @PathVariable String id) {
        categoryService.lambdaUpdate()
                .eq(ProdCategory::getId, id)
                .set(ProdCategory::getDeleted, true)
                .set(ProdCategory::getUpdateTime, LocalDateTime.now())
                .update();
        return ApiResponse.success();
    }
}
