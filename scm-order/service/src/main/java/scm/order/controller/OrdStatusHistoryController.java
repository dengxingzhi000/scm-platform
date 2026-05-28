package scm.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.order.domain.entity.OrdStatusHistory;
import scm.order.service.IOrdStatusHistoryService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/status-history")
@Tag(name = "订单状态历史", description = "订单状态流转历史接口")
public class OrdStatusHistoryController {

    @Autowired
    private IOrdStatusHistoryService statusHistoryService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询状态历史")
    public OrdStatusHistory getById(@PathVariable String id) {
        log.info("[API] 查询状态历史: id={}", id);
        return statusHistoryService.getById(id);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "查询订单的状态历史")
    public List<OrdStatusHistory> listByOrderId(@PathVariable Long orderId) {
        log.info("[API] 查询订单状态历史: orderId={}", orderId);
        return statusHistoryService.listByOrderId(orderId);
    }

    @PostMapping
    @Operation(summary = "创建状态历史记录")
    public boolean save(@RequestBody OrdStatusHistory history) {
        log.info("[API] 创建状态历史: orderId={}, event={}", history.getOrderId(), history.getEvent());
        return statusHistoryService.save(history);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除状态历史记录")
    public boolean delete(@PathVariable String id) {
        log.info("[API] 删除状态历史: id={}", id);
        return statusHistoryService.removeById(id);
    }
}
