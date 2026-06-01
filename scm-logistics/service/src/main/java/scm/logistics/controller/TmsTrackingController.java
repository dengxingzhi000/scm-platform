package scm.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.logistics.domain.entity.TmsTracking;
import scm.logistics.service.ITmsTrackingService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/tms-tracking")
public class TmsTrackingController {

    private final ITmsTrackingService trackingService;

    @GetMapping("/{id}")
    public ApiResponse<TmsTracking> getById(
            @PathVariable String id) {
        TmsTracking tracking = trackingService.getById(id);
        return ApiResponse.success(tracking);
    }

    @GetMapping("/waybill/{waybillId}")
    public ApiResponse<List<TmsTracking>> listByWaybillId(
            @PathVariable String waybillId) {
        List<TmsTracking> list = trackingService.listByWaybillId(waybillId);
        return ApiResponse.success(list);
    }

    @GetMapping("/waybill-no/{waybillNo}")
    public ApiResponse<List<TmsTracking>> listByWaybillNo(
            @PathVariable String waybillNo) {
        List<TmsTracking> list = trackingService.listByWaybillNo(waybillNo);
        return ApiResponse.success(list);
    }

    @GetMapping("/list")
    public ApiResponse<Page<TmsTracking>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String waybillNo,
            @RequestParam(required = false) String trackStatus) {
        Page<TmsTracking> result = trackingService.pageList(page, size, waybillNo, trackStatus);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<TmsTracking> addTracking(@RequestBody TmsTracking tracking) {
        log.info("添加物流轨迹: waybillNo={}, location={}, status={}", tracking.getWaybillNo(), tracking.getLocation(), tracking.getTrackStatus());
        TmsTracking created = trackingService.addTracking(tracking);
        return ApiResponse.success(created);
    }
}
