package scm.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.logistics.domain.entity.TmsTracking;
import scm.logistics.service.ITmsTrackingService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/tms-tracking")
@Tag(name = "物流轨迹管理", description = "物流轨迹的查询与录入")
public class TmsTrackingController {

    @Autowired
    private ITmsTrackingService trackingService;

    @GetMapping("/{id}")
    @Operation(summary = "查询轨迹详情")
    public ApiResponse<TmsTracking> getById(
            @Parameter(description = "轨迹ID", required = true) @PathVariable String id) {
        TmsTracking tracking = trackingService.getById(id);
        return ApiResponse.success(tracking);
    }

    @GetMapping("/waybill/{waybillId}")
    @Operation(summary = "根据运单ID查询轨迹列表")
    public ApiResponse<List<TmsTracking>> listByWaybillId(
            @Parameter(description = "运单ID", required = true) @PathVariable String waybillId) {
        List<TmsTracking> list = trackingService.listByWaybillId(waybillId);
        return ApiResponse.success(list);
    }

    @GetMapping("/waybill-no/{waybillNo}")
    @Operation(summary = "根据运单号查询轨迹列表")
    public ApiResponse<List<TmsTracking>> listByWaybillNo(
            @Parameter(description = "运单号", required = true) @PathVariable String waybillNo) {
        List<TmsTracking> list = trackingService.listByWaybillNo(waybillNo);
        return ApiResponse.success(list);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询轨迹列表")
    public ApiResponse<Page<TmsTracking>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String waybillNo,
            @RequestParam(required = false) String trackStatus) {
        Page<TmsTracking> result = trackingService.pageList(page, size, waybillNo, trackStatus);
        return ApiResponse.success(result);
    }

    @PostMapping
    @Operation(summary = "添加物流轨迹")
    public ApiResponse<TmsTracking> addTracking(@RequestBody TmsTracking tracking) {
        log.info("添加物流轨迹: waybillNo={}, location={}, status={}", tracking.getWaybillNo(), tracking.getLocation(), tracking.getTrackStatus());
        TmsTracking created = trackingService.addTracking(tracking);
        return ApiResponse.success(created);
    }
}
