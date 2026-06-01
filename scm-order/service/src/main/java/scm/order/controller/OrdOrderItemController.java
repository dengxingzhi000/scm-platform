package scm.order.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.order.domain.entity.OrdOrderItem;
import scm.order.service.IOrdOrderItemService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/order-items")
public class OrdOrderItemController {

    private final IOrdOrderItemService orderItemService;

    @GetMapping("/{id}")
    public OrdOrderItem getById(@PathVariable String id) {
        log.info("[API] 查询订单明细: id={}", id);
        return orderItemService.getById(id);
    }

    @GetMapping("/order/{orderId}")
    public List<OrdOrderItem> listByOrderId(@PathVariable Long orderId) {
        log.info("[API] 查询订单明细: orderId={}", orderId);
        return orderItemService.listByOrderId(orderId);
    }

    @PostMapping
    public boolean save(@RequestBody OrdOrderItem item) {
        log.info("[API] 创建订单明细: orderId={}, skuId={}", item.getOrderId(), item.getSkuId());
        return orderItemService.save(item);
    }

    @PutMapping
    public boolean update(@RequestBody OrdOrderItem item) {
        log.info("[API] 更新订单明细: id={}", item.getId());
        return orderItemService.updateById(item);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        log.info("[API] 删除订单明细: id={}", id);
        return orderItemService.removeById(id);
    }
}
