package com.scmcloud.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.domain.PageResult;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.product.domain.entity.ProdSpu;
import com.scmcloud.product.service.IProdSpuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/prod-spu")
@RequiredArgsConstructor
@Tag(name = "SPU 管理", description = "标准商品单元（SPU）的查询、按分类与品牌筛选及增删改接口。")
public class ProdSpuController {
    private final IProdSpuService spuService;

    @Operation(summary = "分页查询 SPU", description = "按关键词、分类、品牌与状态组合条件分页搜索商品SPU。")
    @GetMapping
    public ApiResponse<PageResult<ProdSpu>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键字，可选，匹配商品名称等字段")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "商品分类ID，可选")
            @RequestParam(required = false) String categoryId,
            @Parameter(description = "品牌ID，可选")
            @RequestParam(required = false) String brandId,
            @Parameter(description = "商品状态，可选：0-草稿，1-上架，2-下架，3-删除")
            @RequestParam(required = false) Integer status) {

        Page<ProdSpu> result = spuService.search(keyword, categoryId, brandId, status, page, size);
        return ApiResponse.success(PageResult.of(result));
    }

    @Operation(summary = "查询 SPU 详情", description = "根据SPU ID查询商品SPU的详细信息。")
    @GetMapping("/{id}")
    public ApiResponse<ProdSpu> getById(@Parameter(description = "SPU ID") @PathVariable String id) {
        ProdSpu spu = spuService.getById(id);
        return ApiResponse.success(spu);
    }

    @Operation(summary = "按分类查询 SPU", description = "查询指定商品分类下的全部SPU列表。")
    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<ProdSpu>> listByCategoryId(@Parameter(description = "商品分类ID") @PathVariable String categoryId) {
        List<ProdSpu> result = spuService.listByCategoryId(categoryId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "按品牌查询 SPU", description = "查询指定品牌下的全部SPU列表。")
    @GetMapping("/brand/{brandId}")
    public ApiResponse<List<ProdSpu>> listByBrandId(@Parameter(description = "品牌ID") @PathVariable String brandId) {
        List<ProdSpu> result = spuService.listByBrandId(brandId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "创建 SPU", description = "新增商品SPU，自动生成ID与时间字段，未指定状态时默认为草稿。")
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

    @Operation(summary = "更新 SPU", description = "根据SPU ID更新商品SPU信息并刷新更新时间。")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@Parameter(description = "SPU ID") @PathVariable String id, @RequestBody ProdSpu spu) {
        spu.setId(id);
        spu.setUpdateTime(LocalDateTime.now());
        spuService.updateById(spu);
        return ApiResponse.success();
    }

    @Operation(summary = "删除 SPU", description = "根据SPU ID逻辑删除商品SPU并将其状态置为删除。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "SPU ID") @PathVariable String id) {
        spuService.lambdaUpdate()
                .eq(ProdSpu::getId, id)
                .set(ProdSpu::getDeleted, true)
                .set(ProdSpu::getStatus, 3)
                .set(ProdSpu::getUpdateTime, LocalDateTime.now())
                .update();
        return ApiResponse.success();
    }
}
