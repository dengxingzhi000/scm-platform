package scm.purchase.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurOrderItem;
import scm.purchase.service.IPurOrderItemService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-order-item")
public class PurOrderItemController {

    private final IPurOrderItemService purOrderItemService;

    @GetMapping("/{id}")
    public PurOrderItem getById(@PathVariable String id) {
        return purOrderItemService.getById(id);
    }

    @GetMapping("/list/{orderId}")
    public List<PurOrderItem> listByOrderId(@PathVariable String orderId) {
        return purOrderItemService.listByOrderId(orderId);
    }

    @PostMapping
    public boolean save(@RequestBody PurOrderItem purOrderItem) {
        return purOrderItemService.save(purOrderItem);
    }

    @PutMapping
    public boolean update(@RequestBody PurOrderItem purOrderItem) {
        return purOrderItemService.updateById(purOrderItem);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return purOrderItemService.removeById(id);
    }

    @DeleteMapping("/order/{orderId}")
    public boolean deleteByOrderId(@PathVariable String orderId) {
        return purOrderItemService.deleteByOrderId(orderId);
    }
}
