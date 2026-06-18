package com.scmcloud.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.domain.PageResult;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.product.domain.entity.ProdCategory;
import com.scmcloud.product.service.IProdCategoryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/prod-category")
@RequiredArgsConstructor
public class ProdCategoryController {
    private final IProdCategoryService categoryService;

    @GetMapping
    public ApiResponse<PageResult<ProdCategory>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {

        Page<ProdCategory> result = categoryService.lambdaQuery()
                .like(name != null, ProdCategory::getCategoryName, name)
                .eq(ProdCategory::getDeleted, false)
                .orderByAsc(ProdCategory::getSortOrder)
                .page(new Page<>(page, size));

        return ApiResponse.success(PageResult.of(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProdCategory> getById(@PathVariable String id) {
        ProdCategory category = categoryService.getById(id);
        return ApiResponse.success(category);
    }

    @GetMapping("/tree")
    public ApiResponse<List<ProdCategory>> getCategoryTree() {
        List<ProdCategory> tree = categoryService.getCategoryTree();
        return ApiResponse.success(tree);
    }

    @GetMapping("/children/{parentId}")
    public ApiResponse<List<ProdCategory>> listByParentId(@PathVariable String parentId) {
        List<ProdCategory> children = categoryService.listByParentId(parentId);
        return ApiResponse.success(children);
    }

    @GetMapping("/search")
    public ApiResponse<List<ProdCategory>> search(
            @RequestParam(required = false) String name) {
        List<ProdCategory> result = categoryService.searchByName(name);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody ProdCategory category) {
        category.setId(UUID.randomUUID().toString());
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        category.setDeleted(false);
        categoryService.save(category);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable String id, @RequestBody ProdCategory category) {
        category.setId(id);
        category.setUpdateTime(LocalDateTime.now());
        categoryService.updateById(category);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        categoryService.lambdaUpdate()
                .eq(ProdCategory::getId, id)
                .set(ProdCategory::getDeleted, true)
                .set(ProdCategory::getUpdateTime, LocalDateTime.now())
                .update();
        return ApiResponse.success();
    }
}
