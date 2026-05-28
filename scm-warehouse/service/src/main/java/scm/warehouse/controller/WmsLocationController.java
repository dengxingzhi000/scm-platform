package scm.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import com.frog.common.util.UUIDv7Util;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.warehouse.domain.entity.WmsLocation;
import scm.warehouse.service.IWmsLocationService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/wms-location")
@Tag(name = "库位管理", description = "库位增删改查接口")
public class WmsLocationController {

    @Autowired
    private IWmsLocationService locationService;

    @PostMapping
    @Operation(summary = "创建库位")
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
    @Operation(summary = "更新库位")
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
    @Operation(summary = "删除库位（逻辑删除）")
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
    @Operation(summary = "查询库位详情")
    public ApiResponse<WmsLocation> getById(@PathVariable String id) {
        WmsLocation location = locationService.getById(id);
        if (location == null || Boolean.TRUE.equals(location.getDeleted())) {
            return ApiResponse.fail(404, "库位不存在");
        }
        return ApiResponse.success(location);
    }

    @GetMapping("/list")
    @Operation(summary = "查询仓库下的库位列表")
    public ApiResponse<List<WmsLocation>> listByWarehouseId(
            @Parameter(description = "仓库ID", required = true)
            @RequestParam String warehouseId) {
        return ApiResponse.success(locationService.listByWarehouseId(warehouseId));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询库位")
    public ApiResponse<Page<WmsLocation>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) Integer locationType,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(locationService.pageList(page, size, warehouseId, locationType, status));
    }
}
