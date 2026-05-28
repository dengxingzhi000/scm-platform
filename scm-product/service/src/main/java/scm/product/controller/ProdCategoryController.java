package scm.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.domain.PageResult;
import com.frog.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.product.domain.entity.ProdCategory;
import scm.product.service.IProdCategoryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/prod-category")
@RequiredArgsConstructor
@Tag(name = "商品分类", description = "商品分类管理接口")
public class ProdCategoryController {

    private final IProdCategoryService categoryService;

    @GetMapping
    @Operation(summary = "分页查询分类")
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
    @Operation(summary = "查询分类详情")
    public ApiResponse<ProdCategory> getById(@PathVariable String id) {
        ProdCategory category = categoryService.getById(id);
        return ApiResponse.success(category);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取分类树")
    public ApiResponse<List<ProdCategory>> getCategoryTree() {
        List<ProdCategory> tree = categoryService.getCategoryTree();
        return ApiResponse.success(tree);
    }

    @GetMapping("/children/{parentId}")
    @Operation(summary = "查询子分类")
    public ApiResponse<List<ProdCategory>> listByParentId(@PathVariable String parentId) {
        List<ProdCategory> children = categoryService.listByParentId(parentId);
        return ApiResponse.success(children);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索分类")
    public ApiResponse<List<ProdCategory>> search(
            @Parameter(description = "分类名称") @RequestParam(required = false) String name) {
        List<ProdCategory> result = categoryService.searchByName(name);
        return ApiResponse.success(result);
    }

    @PostMapping
    @Operation(summary = "创建分类")
    public ApiResponse<Void> create(@RequestBody ProdCategory category) {
        category.setId(UUID.randomUUID().toString());
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        category.setDeleted(false);
        categoryService.save(category);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新分类")
    public ApiResponse<Void> update(@PathVariable String id, @RequestBody ProdCategory category) {
        category.setId(id);
        category.setUpdateTime(LocalDateTime.now());
        categoryService.updateById(category);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    public ApiResponse<Void> delete(@PathVariable String id) {
        categoryService.lambdaUpdate()
                .eq(ProdCategory::getId, id)
                .set(ProdCategory::getDeleted, true)
                .set(ProdCategory::getUpdateTime, LocalDateTime.now())
                .update();
        return ApiResponse.success();
    }
}
