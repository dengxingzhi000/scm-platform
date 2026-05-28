package scm.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.order.domain.entity.OrdOrderItem;
import scm.order.service.IOrdOrderItemService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/order-items")
@Tag(name = "订单明细管理", description = "订单明细CRUD接口")
public class OrdOrderItemController {

    @Autowired
    private IOrdOrderItemService orderItemService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询订单明细")
    public OrdOrderItem getById(@PathVariable String id) {
        log.info("[API] 查询订单明细: id={}", id);
        return orderItemService.getById(id);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "查询订单的所有明细")
    public List<OrdOrderItem> listByOrderId(@PathVariable Long orderId) {
        log.info("[API] 查询订单明细: orderId={}", orderId);
        return orderItemService.listByOrderId(orderId);
    }

    @PostMapping
    @Operation(summary = "创建订单明细")
    public boolean save(@RequestBody OrdOrderItem item) {
        log.info("[API] 创建订单明细: orderId={}, skuId={}", item.getOrderId(), item.getSkuId());
        return orderItemService.save(item);
    }

    @PutMapping
    @Operation(summary = "更新订单明细")
    public boolean update(@RequestBody OrdOrderItem item) {
        log.info("[API] 更新订单明细: id={}", item.getId());
        return orderItemService.updateById(item);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除订单明细")
    public boolean delete(@PathVariable String id) {
        log.info("[API] 删除订单明细: id={}", id);
        return orderItemService.removeById(id);
    }
}
