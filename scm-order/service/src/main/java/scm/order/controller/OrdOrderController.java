package scm.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.order.domain.entity.OrdOrder;
import scm.order.domain.entity.OrdOrderItem;
import scm.order.service.IOrdOrderService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "订单管理", description = "订单CRUD接口")
public class OrdOrderController {

    @Autowired
    private IOrdOrderService orderService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询订单")
    public OrdOrder getById(@PathVariable String id) {
        log.info("[API] 查询订单: id={}", id);
        return orderService.getById(id);
    }

    @GetMapping
    @Operation(summary = "查询所有订单")
    public List<OrdOrder> list() {
        log.info("[API] 查询所有订单");
        return orderService.list();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询订单")
    public Page<OrdOrder> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("[API] 分页查询订单: pageNum={}, pageSize={}", pageNum, pageSize);
        return orderService.page(new Page<>(pageNum, pageSize));
    }

    @PostMapping
    @Operation(summary = "创建订单")
    public OrdOrder createOrder(@RequestBody CreateOrderRequest request) {
        log.info("[API] 创建订单: orderNo={}", request.getOrder().getOrderNo());
        return orderService.createOrder(request.getOrder(), request.getItems());
    }

    @PutMapping
    @Operation(summary = "更新订单")
    public boolean update(@RequestBody OrdOrder order) {
        log.info("[API] 更新订单: id={}", order.getId());
        return orderService.updateById(order);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新订单状态")
    public boolean updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        log.info("[API] 更新订单状态: id={}, status={}", id, status);
        return orderService.updateOrderStatus(id, status);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户订单列表")
    public List<OrdOrder> listByUserId(@PathVariable Long userId) {
        log.info("[API] 查询用户订单: userId={}", userId);
        return orderService.listByUserId(userId);
    }

    @GetMapping("/user/{userId}/page")
    @Operation(summary = "分页查询用户订单")
    public Page<OrdOrder> pageByUserId(
            @PathVariable Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("[API] 分页查询用户订单: userId={}, pageNum={}, pageSize={}", userId, pageNum, pageSize);
        return orderService.pageByUserId(userId, pageNum, pageSize);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除订单")
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
