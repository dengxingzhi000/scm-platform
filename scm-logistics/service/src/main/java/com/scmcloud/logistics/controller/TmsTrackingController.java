package com.scmcloud.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.logistics.domain.entity.TmsTracking;
import com.scmcloud.logistics.service.ITmsTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/tms-tracking")
@Tag(name = "物流跟踪", description = "运单物流轨迹的查询与轨迹节点上报接口。")
public class TmsTrackingController {

    private final ITmsTrackingService trackingService;

    @Operation(summary = "查询轨迹详情", description = "根据轨迹主键 ID 获取单条物流轨迹记录。")
    @GetMapping("/{id}")
    public ApiResponse<TmsTracking> getById(
            @Parameter(description = "轨迹记录主键 ID") @PathVariable String id) {
        TmsTracking tracking = trackingService.getById(id);
        return ApiResponse.success(tracking);
    }

    @Operation(summary = "按运单查询轨迹", description = "根据运单主键 ID 获取该运单的全部物流轨迹。")
    @GetMapping("/waybill/{waybillId}")
    public ApiResponse<List<TmsTracking>> listByWaybillId(
            @Parameter(description = "运单主键 ID") @PathVariable String waybillId) {
        List<TmsTracking> list = trackingService.listByWaybillId(waybillId);
        return ApiResponse.success(list);
    }

    @Operation(summary = "按运单号查询轨迹", description = "根据运单号获取该运单的全部物流轨迹。")
    @GetMapping("/waybill-no/{waybillNo}")
    public ApiResponse<List<TmsTracking>> listByWaybillNo(
            @Parameter(description = "运单号") @PathVariable String waybillNo) {
        List<TmsTracking> list = trackingService.listByWaybillNo(waybillNo);
        return ApiResponse.success(list);
    }

    @Operation(summary = "分页查询轨迹", description = "按运单号和轨迹状态分页查询物流轨迹记录。")
    @GetMapping("/list")
    public ApiResponse<Page<TmsTracking>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String waybillNo,
            @RequestParam(required = false) String trackStatus) {
        Page<TmsTracking> result = trackingService.pageList(page, size, waybillNo, trackStatus);
        return ApiResponse.success(result);
    }

    @Operation(summary = "新增物流轨迹", description = "上报一条新的运单物流轨迹节点并返回创建结果。")
    @PostMapping
    public ApiResponse<TmsTracking> addTracking(@RequestBody TmsTracking tracking) {
        log.info("Add tracking: waybillNo={}, location={}, status={}", tracking.getWaybillNo(), tracking.getLocation(), tracking.getTrackStatus());
        TmsTracking created = trackingService.addTracking(tracking);
        return ApiResponse.success(created);
    }
}
