package com.scmcloud.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.logistics.domain.entity.TmsDeliveryArea;
import com.scmcloud.logistics.service.ITmsDeliveryAreaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/tms-delivery-area")
@Tag(name = "配送区域", description = "承运商配送范围区域的维护与覆盖范围校验接口。")
public class TmsDeliveryAreaController {

    private final ITmsDeliveryAreaService deliveryAreaService;

    @Operation(summary = "查询配送区域详情", description = "根据配送区域主键 ID 获取区域详细信息。")
    @GetMapping("/{id}")
    public ApiResponse<TmsDeliveryArea> getById(
            @Parameter(description = "配送区域主键 ID") @PathVariable String id) {
        TmsDeliveryArea area = deliveryAreaService.getById(id);
        return ApiResponse.success(area);
    }

    @Operation(summary = "分页查询配送区域", description = "按承运商、省份和城市分页查询配送区域列表。")
    @GetMapping("/list")
    public ApiResponse<Page<TmsDeliveryArea>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "承运商主键 ID") @RequestParam(required = false) String carrierId,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city) {
        Page<TmsDeliveryArea> result = deliveryAreaService.pageList(page, size, carrierId, province, city);
        return ApiResponse.success(result);
    }

    @Operation(summary = "查询承运商的配送区域", description = "获取指定承运商配置的全部配送区域。")
    @GetMapping("/carrier/{carrierId}")
    public ApiResponse<List<TmsDeliveryArea>> listByCarrier(
            @Parameter(description = "承运商主键 ID") @PathVariable String carrierId) {
        List<TmsDeliveryArea> list = deliveryAreaService.listByCarrier(carrierId);
        return ApiResponse.success(list);
    }

    @Operation(summary = "校验配送覆盖", description = "判断指定承运商是否覆盖给定的省市区地址。")
    @GetMapping("/check-coverage")
    public ApiResponse<Boolean> checkCoverage(
            @Parameter(description = "承运商主键 ID") @RequestParam String carrierId,
            @RequestParam String province,
            @RequestParam String city,
            @Parameter(description = "区县名称，可选，不传则按省市判断") @RequestParam(required = false) String district) {
        boolean covered = deliveryAreaService.checkCoverage(carrierId, province, city, district);
        return ApiResponse.success(covered);
    }

    @Operation(summary = "新增配送区域", description = "创建新的配送区域并返回保存后的区域信息。")
    @PostMapping
    public ApiResponse<TmsDeliveryArea> create(@RequestBody TmsDeliveryArea area) {
        log.info("Create delivery area: areaCode={}, areaName={}", (area.getAreaCode() + "").replace("\r", "_").replace("\n", "_"), (area.getAreaName() + "").replace("\r", "_").replace("\n", "_"));
        area.setId(UUID.randomUUID().toString());
        area.setDeleted(false);
        area.setCreateTime(LocalDateTime.now());
        area.setUpdateTime(LocalDateTime.now());
        deliveryAreaService.save(area);
        return ApiResponse.success(area);
    }

    @Operation(summary = "更新配送区域", description = "根据主键 ID 更新配送区域信息并返回更新后的数据。")
    @PutMapping("/{id}")
    public ApiResponse<TmsDeliveryArea> update(@Parameter(description = "配送区域主键 ID") @PathVariable String id, @RequestBody TmsDeliveryArea area) {
        log.info("Update delivery area: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        area.setId(id);
        area.setUpdateTime(LocalDateTime.now());
        deliveryAreaService.updateById(area);
        return ApiResponse.success(area);
    }

    @Operation(summary = "删除配送区域", description = "根据主键 ID 逻辑删除指定配送区域。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "配送区域主键 ID") @PathVariable String id) {
        log.info("Delete delivery area: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        TmsDeliveryArea area = new TmsDeliveryArea();
        area.setId(id);
        area.setDeleted(true);
        area.setUpdateTime(LocalDateTime.now());
        deliveryAreaService.updateById(area);
        return ApiResponse.success();
    }
}
