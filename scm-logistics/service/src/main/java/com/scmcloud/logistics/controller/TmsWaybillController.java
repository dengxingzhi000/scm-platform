package com.scmcloud.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.logistics.domain.entity.TmsWaybill;
import com.scmcloud.logistics.service.ITmsWaybillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/tms-waybill")
@Tag(name = "运单管理", description = "物流运单的创建、状态流转、取消与查询接口。")
public class TmsWaybillController {

    private final ITmsWaybillService waybillService;

    @Operation(summary = "查询运单详情", description = "根据运单主键 ID 获取运单详细信息。")
    @GetMapping("/{id}")
    public ApiResponse<TmsWaybill> getById(
            @Parameter(description = "运单主键 ID") @PathVariable String id) {
        TmsWaybill waybill = waybillService.getById(id);
        return ApiResponse.success(waybill);
    }

    @Operation(summary = "按运单号查询", description = "根据运单号获取对应的运单信息。")
    @GetMapping("/no/{waybillNo}")
    public ApiResponse<TmsWaybill> getByWaybillNo(
            @Parameter(description = "运单号") @PathVariable String waybillNo) {
        TmsWaybill waybill = waybillService.getByWaybillNo(waybillNo);
        return ApiResponse.success(waybill);
    }

    @Operation(summary = "分页查询运单", description = "按运单号、运单状态和承运商分页查询运单列表。")
    @GetMapping("/list")
    public ApiResponse<Page<TmsWaybill>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String waybillNo,
            @RequestParam(required = false) Integer status,
            @Parameter(description = "承运商主键 ID") @RequestParam(required = false) String carrierId) {
        Page<TmsWaybill> result = waybillService.pageList(page, size, waybillNo, status, carrierId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "按订单查询运单", description = "根据订单主键 ID 获取关联的全部运单。")
    @GetMapping("/order/{orderId}")
    public ApiResponse<List<TmsWaybill>> listByOrderId(
            @Parameter(description = "订单主键 ID") @PathVariable String orderId) {
        List<TmsWaybill> list = waybillService.listByOrderId(orderId);
        return ApiResponse.success(list);
    }

    @Operation(summary = "按订单号查询运单", description = "根据订单号获取关联的全部运单。")
    @GetMapping("/order-no/{orderNo}")
    public ApiResponse<List<TmsWaybill>> listByOrderNo(
            @Parameter(description = "订单号") @PathVariable String orderNo) {
        List<TmsWaybill> list = waybillService.listByOrderNo(orderNo);
        return ApiResponse.success(list);
    }

    @Operation(summary = "创建运单", description = "根据订单与承运商信息创建运单并返回创建结果。")
    @PostMapping
    public ApiResponse<TmsWaybill> create(@RequestBody TmsWaybill waybill) {
        log.info("Create waybill: orderNo={}, carrierId={}", (waybill.getOrderNo() + "").replace("\r", "_").replace("\n", "_"), (waybill.getCarrierId() + "").replace("\r", "_").replace("\n", "_"));
        TmsWaybill created = waybillService.createWaybill(waybill);
        return ApiResponse.success(created);
    }

    @Operation(summary = "更新运单状态", description = "按目标状态更新指定运单的状态，可记录操作人。")
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @Parameter(description = "运单主键 ID") @PathVariable String id,
            @Parameter(description = "目标运单状态码") @RequestParam Integer status,
            @Parameter(description = "操作人标识") @RequestParam(required = false) String operator) {
        log.info("Update waybill status: id={}, status={}", (id + "").replace("\r", "_").replace("\n", "_"), (status + "").replace("\r", "_").replace("\n", "_"));
        boolean success = waybillService.updateStatus(id, status, operator);
        return success ? ApiResponse.success() : ApiResponse.fail(500, "Failed to update waybill status");
    }

    @Operation(summary = "取消运单", description = "取消指定运单并记录取消原因与操作人。")
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @Parameter(description = "运单主键 ID") @PathVariable String id,
            @Parameter(description = "取消原因") @RequestParam(required = false) String reason,
            @Parameter(description = "操作人标识") @RequestParam(required = false) String operator) {
        log.info("Cancel waybill: id={}, reason={}", (id + "").replace("\r", "_").replace("\n", "_"), (reason + "").replace("\r", "_").replace("\n", "_"));
        boolean success = waybillService.cancelWaybill(id, reason, operator);
        return success ? ApiResponse.success() : ApiResponse.fail(500, "Failed to cancel waybill");
    }

    @Operation(summary = "删除运单", description = "根据主键 ID 逻辑删除指定运单。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "运单主键 ID") @PathVariable String id) {
        log.info("Delete waybill: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        TmsWaybill waybill = new TmsWaybill();
        waybill.setId(id);
        waybill.setDeleted(true);
        waybillService.updateById(waybill);
        return ApiResponse.success();
    }
}
