package scm.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import com.frog.common.util.UUIDv7Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.warehouse.domain.entity.WmsWarehouse;
import scm.warehouse.service.IWmsWarehouseService;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-warehouse")
public class WmsWarehouseController {

    private final IWmsWarehouseService warehouseService;

    @PostMapping
    public ApiResponse<WmsWarehouse> create(@RequestBody WmsWarehouse warehouse) {
        log.info("[API] 创建仓库: code={}, name={}", warehouse.getWarehouseCode(), warehouse.getWarehouseName());

        boolean exists = warehouseService.lambdaQuery()
                .eq(WmsWarehouse::getWarehouseCode, warehouse.getWarehouseCode())
                .eq(WmsWarehouse::getDeleted, false)
                .exists();
        if (exists) {
            return ApiResponse.fail(400, "仓库编码已存在: " + warehouse.getWarehouseCode());
        }

        warehouse.setId(UUIDv7Util.generateString());
        warehouse.setEnabled(true);
        warehouse.setUsedCapacity(0);
        warehouse.setDeleted(false);
        warehouse.setCreateTime(LocalDateTime.now());
        warehouse.setUpdateTime(LocalDateTime.now());

        warehouseService.save(warehouse);
        log.info("[API] 仓库创建成功: id={}", warehouse.getId());
        return ApiResponse.success(warehouse);
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsWarehouse> update(@PathVariable String id, @RequestBody WmsWarehouse warehouse) {
        log.info("[API] 更新仓库: id={}", id);

        WmsWarehouse existing = warehouseService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "仓库不存在");
        }

        warehouse.setId(id);
        warehouse.setUpdateTime(LocalDateTime.now());
        warehouseService.updateById(warehouse);
        return ApiResponse.success(warehouseService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除仓库: id={}", id);

        WmsWarehouse existing = warehouseService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "仓库不存在");
        }

        existing.setDeleted(true);
        existing.setUpdateTime(LocalDateTime.now());
        warehouseService.updateById(existing);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsWarehouse> getById(@PathVariable String id) {
        WmsWarehouse warehouse = warehouseService.getById(id);
        if (warehouse == null || Boolean.TRUE.equals(warehouse.getDeleted())) {
            return ApiResponse.fail(404, "仓库不存在");
        }
        return ApiResponse.success(warehouse);
    }

    @GetMapping("/list")
    public ApiResponse<List<WmsWarehouse>> listEnabled() {
        return ApiResponse.success(warehouseService.listEnabled());
    }

    @GetMapping("/page")
    public ApiResponse<Page<WmsWarehouse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseName,
            @RequestParam(required = false) Integer warehouseType,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(warehouseService.pageList(page, size, warehouseName, warehouseType, enabled));
    }

    @PutMapping("/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable String id) {
        log.info("[API] 启用仓库: id={}", id);
        boolean success = warehouseService.enable(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "启用失败");
    }

    @PutMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable String id) {
        log.info("[API] 停用仓库: id={}", id);
        boolean success = warehouseService.disable(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "停用失败");
    }
}
