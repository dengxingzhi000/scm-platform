package scm.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.logistics.domain.entity.TmsWaybill;
import scm.logistics.service.ITmsWaybillService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/tms-waybill")
@Tag(name = "运单管理", description = "运单的创建、查询、状态跟踪")
public class TmsWaybillController {

    @Autowired
    private ITmsWaybillService waybillService;

    @GetMapping("/{id}")
    @Operation(summary = "查询运单详情")
    public ApiResponse<TmsWaybill> getById(
            @Parameter(description = "运单ID", required = true) @PathVariable String id) {
        TmsWaybill waybill = waybillService.getById(id);
        return ApiResponse.success(waybill);
    }

    @GetMapping("/no/{waybillNo}")
    @Operation(summary = "根据运单号查询")
    public ApiResponse<TmsWaybill> getByWaybillNo(
            @Parameter(description = "运单号", required = true) @PathVariable String waybillNo) {
        TmsWaybill waybill = waybillService.getByWaybillNo(waybillNo);
        return ApiResponse.success(waybill);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询运单列表")
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
    @Operation(summary = "根据订单ID查询运单")
    public ApiResponse<List<TmsWaybill>> listByOrderId(
            @Parameter(description = "订单ID", required = true) @PathVariable String orderId) {
        List<TmsWaybill> list = waybillService.listByOrderId(orderId);
        return ApiResponse.success(list);
    }

    @GetMapping("/order-no/{orderNo}")
    @Operation(summary = "根据订单号查询运单")
    public ApiResponse<List<TmsWaybill>> listByOrderNo(
            @Parameter(description = "订单号", required = true) @PathVariable String orderNo) {
        List<TmsWaybill> list = waybillService.listByOrderNo(orderNo);
        return ApiResponse.success(list);
    }

    @PostMapping
    @Operation(summary = "创建运单")
    public ApiResponse<TmsWaybill> create(@RequestBody TmsWaybill waybill) {
        log.info("创建运单: orderNo={}, carrierId={}", waybill.getOrderNo(), waybill.getCarrierId());
        TmsWaybill created = waybillService.createWaybill(waybill);
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新运单状态")
    public ApiResponse<Void> updateStatus(
            @PathVariable String id,
            @RequestParam Integer status,
            @RequestParam(required = false) String operator) {
        log.info("更新运单状态: id={}, status={}", id, status);
        boolean success = waybillService.updateStatus(id, status, operator);
        return success ? ApiResponse.success() : ApiResponse.fail(500, "更新运单状态失败");
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消运单")
    public ApiResponse<Void> cancel(
            @PathVariable String id,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String operator) {
        log.info("取消运单: id={}, reason={}", id, reason);
        boolean success = waybillService.cancelWaybill(id, reason, operator);
        return success ? ApiResponse.success() : ApiResponse.fail(500, "取消运单失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除运单（逻辑删除）")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("删除运单: id={}", id);
        TmsWaybill waybill = new TmsWaybill();
        waybill.setId(id);
        waybill.setDeleted(true);
        waybillService.updateById(waybill);
        return ApiResponse.success();
    }
}
