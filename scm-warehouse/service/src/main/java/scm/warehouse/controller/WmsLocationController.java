package scm.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import com.frog.common.util.UUIDv7Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.warehouse.domain.entity.WmsLocation;
import scm.warehouse.service.IWmsLocationService;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-location")
public class WmsLocationController {

    private final IWmsLocationService locationService;

    @PostMapping
    public ApiResponse<WmsLocation> create(@RequestBody WmsLocation location) {
        log.info("[API] 创建库位: warehouseId={}, code={}", location.getWarehouseId(), location.getLocationCode());

        boolean exists = locationService.lambdaQuery()
                .eq(WmsLocation::getWarehouseId, location.getWarehouseId())
                .eq(WmsLocation::getLocationCode, location.getLocationCode())
                .eq(WmsLocation::getDeleted, false)
                .exists();
        if (exists) {
            return ApiResponse.fail(400, "同一仓库下库位编码已存在: " + location.getLocationCode());
        }

        location.setId(UUIDv7Util.generateString());
        location.setCurrentCapacity(0);
        location.setStatus(1); // 1-可用
        location.setEnabled(true);
        location.setDeleted(false);
        location.setCreateTime(LocalDateTime.now());
        location.setUpdateTime(LocalDateTime.now());

        locationService.save(location);
        log.info("[API] 库位创建成功: id={}", location.getId());
        return ApiResponse.success(location);
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsLocation> update(@PathVariable String id, @RequestBody WmsLocation location) {
        log.info("[API] 更新库位: id={}", id);

        WmsLocation existing = locationService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "库位不存在");
        }

        location.setId(id);
        location.setUpdateTime(LocalDateTime.now());
        locationService.updateById(location);
        return ApiResponse.success(locationService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除库位: id={}", id);

        WmsLocation existing = locationService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "库位不存在");
        }

        existing.setDeleted(true);
        existing.setUpdateTime(LocalDateTime.now());
        locationService.updateById(existing);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsLocation> getById(@PathVariable String id) {
        WmsLocation location = locationService.getById(id);
        if (location == null || Boolean.TRUE.equals(location.getDeleted())) {
            return ApiResponse.fail(404, "库位不存在");
        }
        return ApiResponse.success(location);
    }

    @GetMapping("/list")
    public ApiResponse<List<WmsLocation>> listByWarehouseId(
            @RequestParam String warehouseId) {
        return ApiResponse.success(locationService.listByWarehouseId(warehouseId));
    }

    @GetMapping("/page")
    public ApiResponse<Page<WmsLocation>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) Integer locationType,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(locationService.pageList(page, size, warehouseId, locationType, status));
    }
}
