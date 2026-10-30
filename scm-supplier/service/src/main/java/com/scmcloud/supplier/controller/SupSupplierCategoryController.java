package com.scmcloud.supplier.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.supplier.domain.entity.SupSupplierCategory;
import com.scmcloud.supplier.service.ISupSupplierCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/supplier-categories")
@Tag(name = "供应商分类字典", description = "维护供应商分类树，供供应商画像与筛选使用。")
public class SupSupplierCategoryController {

    private final ISupSupplierCategoryService categoryService;

    @Operation(summary = "查询分类列表", description = "按层级与排序返回分类字典，可按关键字与启用状态过滤。")
    @GetMapping
    public ApiResponse<List<SupSupplierCategory>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(categoryService.listCategories(keyword, enabled));
    }

    @Operation(summary = "新增分类", description = "新增供应商分类，编码在租户内唯一。")
    @PostMapping
    public ApiResponse<SupSupplierCategory> create(@RequestBody SupSupplierCategory category) {
        return ApiResponse.success(categoryService.createCategory(category));
    }

    @Operation(summary = "更新分类", description = "更新分类名称、上级与排序。")
    @PutMapping("/{id}")
    public ApiResponse<SupSupplierCategory> update(@Parameter(description = "分类ID") @PathVariable String id,
                                                   @RequestBody SupSupplierCategory category) {
        return ApiResponse.success(categoryService.updateCategory(id, category));
    }

    @Operation(summary = "删除分类", description = "删除未被供应商引用的分类。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "分类ID") @PathVariable String id) {
        categoryService.deleteCategory(id);
        return ApiResponse.success();
    }
}
