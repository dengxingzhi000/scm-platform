package scm.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.order.domain.entity.OrdOrder;
import scm.order.domain.entity.OrdOrderItem;
import scm.order.service.IOrdOrderService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
public class OrdOrderController {

    private final IOrdOrderService orderService;

    @GetMapping("/{id}")
    public OrdOrder getById(@PathVariable String id) {
        log.info("[API] 查询订单: id={}", id);
        return orderService.getById(id);
    }

    @GetMapping
    public List<OrdOrder> list() {
        log.info("[API] 查询所有订单");
        return orderService.list();
    }

    @GetMapping("/page")
    public Page<OrdOrder> page(
@RequestParam(defaultValue = "1") Integer pageNum,
@RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("[API] 分页查询订单: pageNum={}, pageSize={}", pageNum, pageSize);
        return orderService.page(new Page<>(pageNum, pageSize));
    }

    @PostMapping
    public OrdOrder createOrder(@RequestBody CreateOrderRequest request) {
        log.info("[API] 创建订单: orderNo={}", request.getOrder().getOrderNo());
        return orderService.createOrder(request.getOrder(), request.getItems());
    }

    @PutMapping
    public boolean update(@RequestBody OrdOrder order) {
        log.info("[API] 更新订单: id={}", order.getId());
        return orderService.updateById(order);
    }

    @PutMapping("/{id}/status")
    public boolean updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        log.info("[API] 更新订单状态: id={}, status={}", id, status);
        return orderService.updateOrderStatus(id, status);
    }

    @GetMapping("/user/{userId}")
    public List<OrdOrder> listByUserId(@PathVariable Long userId) {
        log.info("[API] 查询用户订单: userId={}", userId);
        return orderService.listByUserId(userId);
    }

    @GetMapping("/user/{userId}/page")
    public Page<OrdOrder> pageByUserId(
            @PathVariable Long userId,
@RequestParam(defaultValue = "1") Integer pageNum,
@RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("[API] 分页查询用户订单: userId={}, pageNum={}, pageSize={}", userId, pageNum, pageSize);
        return orderService.pageByUserId(userId, pageNum, pageSize);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        log.info("[API] 删除订单: id={}", id);
        return orderService.removeById(id);
    }

    public static class CreateOrderRequest {
        private OrdOrder order;
        private List<OrdOrderItem> items;

        public OrdOrder getOrder() { return order; }
        public void setOrder(OrdOrder order) { this.order = order; }
        public List<OrdOrderItem> getItems() { return items; }
        public void setItems(List<OrdOrderItem> items) { this.items = items; }
    }
}
