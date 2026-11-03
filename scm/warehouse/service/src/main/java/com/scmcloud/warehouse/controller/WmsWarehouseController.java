package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsWarehouse;
import com.scmcloud.warehouse.service.command.WmsWarehouseCommandService;
import com.scmcloud.warehouse.service.query.WmsWarehouseQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-warehouse")
@Tag(name = "仓库管理", description = "仓库档案的创建、维护、启停用与查询管理")
public class WmsWarehouseController {

    private final WmsWarehouseCommandService warehouseCommandService;
    private final WmsWarehouseQueryService warehouseQueryService;

    @Operation(summary = "创建仓库", description = "新建仓库档案，仓库编码需保持唯一。")
    @PostMapping
    public ApiResponse<WmsWarehouse> create(@RequestBody WmsWarehouse warehouse) {
        log.info("[API] 创建仓库: code={}, name={}", (warehouse.getWarehouseCode() + "").replace("\r", "_").replace("\n", "_"), (warehouse.getWarehouseName() + "").replace("\r", "_").replace("\n", "_"));
        try {
            WmsWarehouse created = warehouseCommandService.create(warehouse);
            log.info("[API] 仓库创建成功: id={}", (created.getId() + "").replace("\r", "_").replace("\n", "_"));
            return ApiResponse.success(created);
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "更新仓库", description = "按主键更新仓库的基础信息与联系人信息。")
    @PutMapping("/{id}")
    public ApiResponse<WmsWarehouse> update(@Parameter(description = "仓库主键 ID") @PathVariable String id, @RequestBody WmsWarehouse warehouse) {
        log.info("[API] 更新仓库: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        warehouse.setId(id);
        boolean success = warehouseCommandService.update(warehouse);
        if (!success) {
            return ApiResponse.fail(404, "仓库不存在");
        }
        return ApiResponse.success(warehouseQueryService.getById(id));
    }

    @Operation(summary = "删除仓库", description = "按主键逻辑删除仓库，已删除的仓库不再参与出入库作业。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "仓库主键 ID") @PathVariable String id) {
        log.info("[API] 删除仓库: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        boolean success = warehouseCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "仓库不存在");
    }

    @Operation(summary = "查询仓库详情", description = "按主键查询仓库，仓库不存在或已删除时返回 404。")
    @GetMapping("/{id}")
    public ApiResponse<WmsWarehouse> getById(@Parameter(description = "仓库主键 ID") @PathVariable String id) {
        WmsWarehouse warehouse = warehouseQueryService.getById(id);
        if (warehouse == null || Boolean.TRUE.equals(warehouse.getDeleted())) {
            return ApiResponse.fail(404, "仓库不存在");
        }
        return ApiResponse.success(warehouse);
    }

    @Operation(summary = "查询启用仓库列表", description = "查询全部处于启用状态的仓库，用于下拉选择。")
    @GetMapping("/list")
    public ApiResponse<List<WmsWarehouse>> listEnabled() {
        return ApiResponse.success(warehouseQueryService.listEnabled());
    }

    @Operation(summary = "分页查询仓库", description = "按仓库名称、仓库类型与启用状态分页查询仓库列表。")
    @GetMapping("/page")
    public ApiResponse<Page<WmsWarehouse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "仓库名称，支持模糊匹配") @RequestParam(required = false) String warehouseName,
            @Parameter(description = "仓库类型编码") @RequestParam(required = false) Integer warehouseType,
            @Parameter(description = "是否启用，true 为启用、false 为停用") @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(warehouseQueryService.pageList(page, size, warehouseName, warehouseType, enabled));
    }

    @Operation(summary = "启用仓库", description = "将指定仓库置为启用状态，启用后可参与出入库作业。")
    @PutMapping("/{id}/enable")
    public ApiResponse<Void> enable(@Parameter(description = "仓库主键 ID") @PathVariable String id) {
        log.info("[API] 启用仓库: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        boolean success = warehouseCommandService.enable(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "启用失败");
    }

    @Operation(summary = "停用仓库", description = "将指定仓库置为停用状态，停用后不再接收新的出入库作业。")
    @PutMapping("/{id}/disable")
    public ApiResponse<Void> disable(@Parameter(description = "仓库主键 ID") @PathVariable String id) {
        log.info("[API] 停用仓库: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        boolean success = warehouseCommandService.disable(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "停用失败");
    }
}
