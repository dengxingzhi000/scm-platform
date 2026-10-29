package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsLocation;
import com.scmcloud.warehouse.service.command.WmsLocationCommandService;
import com.scmcloud.warehouse.service.query.WmsLocationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-location")
@Tag(name = "库位管理", description = "仓库库位档案的创建、维护与查询管理")
public class WmsLocationController {

    private final WmsLocationCommandService locationCommandService;
    private final WmsLocationQueryService locationQueryService;

    @Operation(summary = "创建库位", description = "在指定仓库下新建库位，库位编码需保持唯一。")
    @PostMapping
    public ApiResponse<WmsLocation> create(@RequestBody WmsLocation location) {
        log.info("[API] 创建库位: warehouseId={}, code={}", (location.getWarehouseId() + "").replace("\r", "_").replace("\n", "_"), (location.getLocationCode() + "").replace("\r", "_").replace("\n", "_"));
        try {
            WmsLocation created = locationCommandService.create(location);
            log.info("[API] 库位创建成功: id={}", (created.getId() + "").replace("\r", "_").replace("\n", "_"));
            return ApiResponse.success(created);
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "更新库位", description = "按主键更新库位的基础信息与状态。")
    @PutMapping("/{id}")
    public ApiResponse<WmsLocation> update(@Parameter(description = "库位主键 ID") @PathVariable String id, @RequestBody WmsLocation location) {
        log.info("[API] 更新库位: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        location.setId(id);
        boolean success = locationCommandService.update(location);
        if (!success) {
            return ApiResponse.fail(404, "库位不存在");
        }
        return ApiResponse.success(locationQueryService.getById(id));
    }

    @Operation(summary = "删除库位", description = "按主键逻辑删除库位，已删除的库位不再参与上架分配。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "库位主键 ID") @PathVariable String id) {
        log.info("[API] 删除库位: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        boolean success = locationCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "库位不存在");
    }

    @Operation(summary = "查询库位详情", description = "按主键查询库位，库位不存在或已删除时返回 404。")
    @GetMapping("/{id}")
    public ApiResponse<WmsLocation> getById(@Parameter(description = "库位主键 ID") @PathVariable String id) {
        WmsLocation location = locationQueryService.getById(id);
        if (location == null || Boolean.TRUE.equals(location.getDeleted())) {
            return ApiResponse.fail(404, "库位不存在");
        }
        return ApiResponse.success(location);
    }

    @Operation(summary = "查询仓库库位列表", description = "根据仓库 ID 查询该仓库下的全部库位。")
    @GetMapping("/list")
    public ApiResponse<List<WmsLocation>> listByWarehouseId(
            @Parameter(description = "仓库主键 ID") @RequestParam String warehouseId) {
        return ApiResponse.success(locationQueryService.listByWarehouseId(warehouseId));
    }

    @Operation(summary = "分页查询库位", description = "按仓库、库位类型与状态分页查询库位列表。")
    @GetMapping("/page")
    public ApiResponse<Page<WmsLocation>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "仓库 ID，用于按仓库过滤") @RequestParam(required = false) String warehouseId,
            @Parameter(description = "库位类型编码") @RequestParam(required = false) Integer locationType,
            @Parameter(description = "库位状态编码") @RequestParam(required = false) Integer status) {
        return ApiResponse.success(locationQueryService.pageList(page, size, warehouseId, locationType, status));
    }
}
