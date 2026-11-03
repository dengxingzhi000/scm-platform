package com.scmcloud.purchase.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.purchase.domain.entity.PurOrderItem;
import com.scmcloud.purchase.service.IPurOrderItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-order-item")
@Tag(name = "采购订单明细", description = "采购订单明细行的查询与维护。")
public class PurOrderItemController {

    private final IPurOrderItemService purOrderItemService;

    @Operation(summary = "根据 ID 查询订单明细", description = "按主键查询单条采购订单明细。")
    @GetMapping("/{id}")
    public PurOrderItem getById(@Parameter(description = "采购订单明细 ID") @PathVariable String id) {
        return purOrderItemService.getById(id);
    }

    @Operation(summary = "按订单查询明细列表", description = "查询指定采购订单下的全部明细行。")
    @GetMapping("/list/{orderId}")
    public List<PurOrderItem> listByOrderId(@Parameter(description = "采购订单 ID") @PathVariable String orderId) {
        return purOrderItemService.listByOrderId(orderId);
    }

    @Operation(summary = "新增订单明细", description = "为采购订单新增一条明细行。")
    @PostMapping
    public boolean save(@RequestBody PurOrderItem purOrderItem) {
        return purOrderItemService.save(purOrderItem);
    }

    @Operation(summary = "更新订单明细", description = "按主键更新采购订单明细信息。")
    @PutMapping
    public boolean update(@RequestBody PurOrderItem purOrderItem) {
        return purOrderItemService.updateById(purOrderItem);
    }

    @Operation(summary = "删除订单明细", description = "按主键删除单条采购订单明细。")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "采购订单明细 ID") @PathVariable String id) {
        return purOrderItemService.removeById(id);
    }

    @Operation(summary = "按订单删除明细", description = "删除指定采购订单下的全部明细行。")
    @DeleteMapping("/order/{orderId}")
    public boolean deleteByOrderId(@Parameter(description = "采购订单 ID") @PathVariable String orderId) {
        return purOrderItemService.deleteByOrderId(orderId);
    }
}
