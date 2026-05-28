package scm.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.logistics.domain.entity.TmsDeliveryArea;
import scm.logistics.service.ITmsDeliveryAreaService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tms-delivery-area")
@Tag(name = "配送区域管理", description = "配送区域的增删改查及覆盖检查")
public class TmsDeliveryAreaController {

    @Autowired
    private ITmsDeliveryAreaService deliveryAreaService;

    @GetMapping("/{id}")
    @Operation(summary = "查询配送区域详情")
    public ApiResponse<TmsDeliveryArea> getById(
            @Parameter(description = "区域ID", required = true) @PathVariable String id) {
        TmsDeliveryArea area = deliveryAreaService.getById(id);
        return ApiResponse.success(area);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询配送区域列表")
    public ApiResponse<Page<TmsDeliveryArea>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String carrierId,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city) {
        Page<TmsDeliveryArea> result = deliveryAreaService.pageList(page, size, carrierId, province, city);
        return ApiResponse.success(result);
    }

    @GetMapping("/carrier/{carrierId}")
    @Operation(summary = "根据物流商查询配送区域")
    public ApiResponse<List<TmsDeliveryArea>> listByCarrier(
            @Parameter(description = "物流商ID", required = true) @PathVariable String carrierId) {
        List<TmsDeliveryArea> list = deliveryAreaService.listByCarrier(carrierId);
        return ApiResponse.success(list);
    }

    @GetMapping("/check-coverage")
    @Operation(summary = "检查区域覆盖")
    public ApiResponse<Boolean> checkCoverage(
            @Parameter(description = "物流商ID", required = true) @RequestParam String carrierId,
            @Parameter(description = "省", required = true) @RequestParam String province,
            @Parameter(description = "市", required = true) @RequestParam String city,
            @Parameter(description = "区/县") @RequestParam(required = false) String district) {
        boolean covered = deliveryAreaService.checkCoverage(carrierId, province, city, district);
        return ApiResponse.success(covered);
    }

    @PostMapping
    @Operation(summary = "新增配送区域")
    public ApiResponse<TmsDeliveryArea> create(@RequestBody TmsDeliveryArea area) {
        log.info("新增配送区域: areaCode={}, areaName={}", area.getAreaCode(), area.getAreaName());
        area.setId(UUID.randomUUID().toString());
        area.setDeleted(false);
        area.setCreateTime(LocalDateTime.now());
        area.setUpdateTime(LocalDateTime.now());
        deliveryAreaService.save(area);
        return ApiResponse.success(area);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改配送区域")
    public ApiResponse<TmsDeliveryArea> update(@PathVariable String id, @RequestBody TmsDeliveryArea area) {
        log.info("修改配送区域: id={}", id);
        area.setId(id);
        area.setUpdateTime(LocalDateTime.now());
        deliveryAreaService.updateById(area);
        return ApiResponse.success(area);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配送区域（逻辑删除）")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("删除配送区域: id={}", id);
        TmsDeliveryArea area = new TmsDeliveryArea();
        area.setId(id);
        area.setDeleted(true);
        area.setUpdateTime(LocalDateTime.now());
        deliveryAreaService.updateById(area);
        return ApiResponse.success();
    }
}
