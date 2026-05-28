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
import scm.product.domain.entity.ProdSpu;
import scm.product.service.IProdSpuService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/prod-spu")
@RequiredArgsConstructor
@Tag(name = "SPU管理", description = "SPU标准产品单元管理接口")
public class ProdSpuController {

    private final IProdSpuService spuService;

    @GetMapping
    @Operation(summary = "分页查询SPU")
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
    @Operation(summary = "查询SPU详情")
    public ApiResponse<ProdSpu> getById(@PathVariable String id) {
        ProdSpu spu = spuService.getById(id);
        return ApiResponse.success(spu);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "查询分类下的SPU")
    public ApiResponse<List<ProdSpu>> listByCategoryId(@PathVariable String categoryId) {
        List<ProdSpu> result = spuService.listByCategoryId(categoryId);
        return ApiResponse.success(result);
    }

    @GetMapping("/brand/{brandId}")
    @Operation(summary = "查询品牌下的SPU")
    public ApiResponse<List<ProdSpu>> listByBrandId(@PathVariable String brandId) {
        List<ProdSpu> result = spuService.listByBrandId(brandId);
        return ApiResponse.success(result);
    }

    @PostMapping
    @Operation(summary = "创建SPU")
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
    @Operation(summary = "更新SPU")
    public ApiResponse<Void> update(@PathVariable String id, @RequestBody ProdSpu spu) {
        spu.setId(id);
        spu.setUpdateTime(LocalDateTime.now());
        spuService.updateById(spu);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除SPU")
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
