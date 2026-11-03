package com.cognora.scm.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.scm.purchase.domain.entity.PurOrder;
import com.cognora.scm.purchase.domain.entity.PurOrderItem;
import com.cognora.scm.purchase.service.IPurOrderItemService;
import com.cognora.scm.purchase.service.query.PurOrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 采购订单读接口。列表页与详情页使用，写入由采购流程负责。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/pur-order")
@Tag(name = "采购订单", description = "采购订单的列表、详情与明细查询。")
public class PurOrderController {

    private final PurOrderQueryService purOrderQueryService;
    private final IPurOrderItemService purOrderItemService;

    @Operation(summary = "分页查询采购订单", description = "按单号/供应商关键字、状态与供应商分页查询采购订单。")
    @GetMapping("/page")
    public ApiResponse<Page<PurOrder>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "采购单号或供应商名称关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "采购订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "供应商 ID") @RequestParam(required = false) String supplierId) {
        return ApiResponse.success(purOrderQueryService.pageList(page, size, keyword, status, supplierId));
    }

    @Operation(summary = "查询采购订单详情", description = "按主键查询采购订单，不存在或已删除时返回 404。")
    @GetMapping("/{id}")
    public ApiResponse<PurOrder> getById(@Parameter(description = "采购订单 ID") @PathVariable String id) {
        PurOrder order = purOrderQueryService.getById(id);
        if (order == null || Boolean.TRUE.equals(order.getDeleted())) {
            return ApiResponse.fail(404, "采购订单不存在");
        }
        return ApiResponse.success(order);
    }

    @Operation(summary = "查询采购订单明细", description = "查询指定采购订单下的全部明细行。")
    @GetMapping("/{id}/items")
    public ApiResponse<List<PurOrderItem>> listItems(@Parameter(description = "采购订单 ID") @PathVariable String id) {
        return ApiResponse.success(purOrderItemService.listByOrderId(id));
    }
}
