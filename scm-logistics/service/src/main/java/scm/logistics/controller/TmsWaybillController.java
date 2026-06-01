package scm.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.logistics.domain.entity.TmsWaybill;
import scm.logistics.service.ITmsWaybillService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/tms-waybill")
public class TmsWaybillController {

    private final ITmsWaybillService waybillService;

    @GetMapping("/{id}")
    public ApiResponse<TmsWaybill> getById(
            @PathVariable String id) {
        TmsWaybill waybill = waybillService.getById(id);
        return ApiResponse.success(waybill);
    }

    @GetMapping("/no/{waybillNo}")
    public ApiResponse<TmsWaybill> getByWaybillNo(
            @PathVariable String waybillNo) {
        TmsWaybill waybill = waybillService.getByWaybillNo(waybillNo);
        return ApiResponse.success(waybill);
    }

    @GetMapping("/list")
    public ApiResponse<Page<TmsWaybill>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String waybillNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String carrierId) {
        Page<TmsWaybill> result = waybillService.pageList(page, size, waybillNo, status, carrierId);
        return ApiResponse.success(result);
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<List<TmsWaybill>> listByOrderId(
            @PathVariable String orderId) {
        List<TmsWaybill> list = waybillService.listByOrderId(orderId);
        return ApiResponse.success(list);
    }

    @GetMapping("/order-no/{orderNo}")
    public ApiResponse<List<TmsWaybill>> listByOrderNo(
            @PathVariable String orderNo) {
        List<TmsWaybill> list = waybillService.listByOrderNo(orderNo);
        return ApiResponse.success(list);
    }

    @PostMapping
    public ApiResponse<TmsWaybill> create(@RequestBody TmsWaybill waybill) {
        log.info("创建运单: orderNo={}, carrierId={}", waybill.getOrderNo(), waybill.getCarrierId());
        TmsWaybill created = waybillService.createWaybill(waybill);
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @PathVariable String id,
            @RequestParam Integer status,
            @RequestParam(required = false) String operator) {
        log.info("更新运单状�? id={}, status={}", id, status);
        boolean success = waybillService.updateStatus(id, status, operator);
        return success ? ApiResponse.success() : ApiResponse.fail(500, "更新运单状态失败");
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @PathVariable String id,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String operator) {
        log.info("取消运单: id={}, reason={}", id, reason);
        boolean success = waybillService.cancelWaybill(id, reason, operator);
        return success ? ApiResponse.success() : ApiResponse.fail(500, "取消运单失败");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("删除运单: id={}", id);
        TmsWaybill waybill = new TmsWaybill();
        waybill.setId(id);
        waybill.setDeleted(true);
        waybillService.updateById(waybill);
        return ApiResponse.success();
    }
}
