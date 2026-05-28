package scm.purchase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurOrderItem;
import scm.purchase.service.IPurOrderItemService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pur-order-item")
@Tag(name = "采购订单明细管理", description = "采购订单明细CRUD接口")
public class PurOrderItemController {

    @Autowired
    private IPurOrderItemService purOrderItemService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询明细")
    public PurOrderItem getById(@PathVariable String id) {
        return purOrderItemService.getById(id);
    }

    @GetMapping("/list/{orderId}")
    @Operation(summary = "根据订单ID查询明细列表")
    public List<PurOrderItem> listByOrderId(@PathVariable String orderId) {
        return purOrderItemService.listByOrderId(orderId);
    }

    @PostMapping
    @Operation(summary = "创建明细")
    public boolean save(@RequestBody PurOrderItem purOrderItem) {
        return purOrderItemService.save(purOrderItem);
    }

    @PutMapping
    @Operation(summary = "更新明细")
    public boolean update(@RequestBody PurOrderItem purOrderItem) {
        return purOrderItemService.updateById(purOrderItem);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除明细")
    public boolean delete(@PathVariable String id) {
        return purOrderItemService.removeById(id);
    }

    @DeleteMapping("/order/{orderId}")
    @Operation(summary = "根据订单ID删除所有明细")
    public boolean deleteByOrderId(@PathVariable String orderId) {
        return purOrderItemService.deleteByOrderId(orderId);
    }
}
