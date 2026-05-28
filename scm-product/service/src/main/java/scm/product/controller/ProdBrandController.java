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
import scm.product.domain.entity.ProdBrand;
import scm.product.service.IProdBrandService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/prod-brand")
@RequiredArgsConstructor
@Tag(name = "商品品牌", description = "商品品牌管理接口")
public class ProdBrandController {

    private final IProdBrandService brandService;

    @GetMapping
    @Operation(summary = "分页查询品牌")
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
    @Operation(summary = "查询品牌详情")
    public ApiResponse<ProdBrand> getById(@PathVariable String id) {
        ProdBrand brand = brandService.getById(id);
        return ApiResponse.success(brand);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索品牌")
    public ApiResponse<List<ProdBrand>> search(
            @Parameter(description = "品牌名称") @RequestParam(required = false) String name) {
        List<ProdBrand> result = brandService.searchByName(name);
        return ApiResponse.success(result);
    }

    @GetMapping("/featured")
    @Operation(summary = "查询推荐品牌")
    public ApiResponse<List<ProdBrand>> listFeatured() {
        List<ProdBrand> result = brandService.listFeatured();
        return ApiResponse.success(result);
    }

    @PostMapping
    @Operation(summary = "创建品牌")
    public ApiResponse<Void> create(@RequestBody ProdBrand brand) {
        brand.setId(UUID.randomUUID().toString());
        brand.setCreateTime(LocalDateTime.now());
        brand.setUpdateTime(LocalDateTime.now());
        brand.setDeleted(false);
        brandService.save(brand);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新品牌")
    public ApiResponse<Void> update(@PathVariable String id, @RequestBody ProdBrand brand) {
        brand.setId(id);
        brand.setUpdateTime(LocalDateTime.now());
        brandService.updateById(brand);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除品牌")
    public ApiResponse<Void> delete(@PathVariable String id) {
        brandService.lambdaUpdate()
                .eq(ProdBrand::getId, id)
                .set(ProdBrand::getDeleted, true)
                .set(ProdBrand::getUpdateTime, LocalDateTime.now())
                .update();
        return ApiResponse.success();
    }
}
