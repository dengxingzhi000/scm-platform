package scm.order.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.order.domain.entity.OrdStatusHistory;
import scm.order.service.IOrdStatusHistoryService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/status-history")
public class OrdStatusHistoryController {

    private final IOrdStatusHistoryService statusHistoryService;

    @GetMapping("/{id}")
    public OrdStatusHistory getById(@PathVariable String id) {
        log.info("[API] 查询状态历史: id={}", id);
        return statusHistoryService.getById(id);
    }

    @GetMapping("/order/{orderId}")
    public List<OrdStatusHistory> listByOrderId(@PathVariable Long orderId) {
        log.info("[API] 查询订单状态历史: orderId={}", orderId);
        return statusHistoryService.listByOrderId(orderId);
    }

    @PostMapping
    public boolean save(@RequestBody OrdStatusHistory history) {
        log.info("[API] 创建状态历史: orderId={}, event={}", history.getOrderId(), history.getEvent());
        return statusHistoryService.save(history);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        log.info("[API] 删除状态历史: id={}", id);
        return statusHistoryService.removeById(id);
    }
}
