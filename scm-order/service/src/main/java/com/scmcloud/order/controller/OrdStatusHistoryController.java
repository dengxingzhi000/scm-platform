package com.scmcloud.order.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.order.domain.entity.OrdStatusHistory;
import com.scmcloud.order.service.IOrdStatusHistoryService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/status-history")
public class OrdStatusHistoryController {

    private final IOrdStatusHistoryService statusHistoryService;

    @GetMapping("/{id}")
    public OrdStatusHistory getById(@PathVariable String id) {
        log.info("[API] Query status history: id={}", id);
        return statusHistoryService.getById(id);
    }

    @GetMapping("/order/{orderId}")
    public List<OrdStatusHistory> listByOrderId(@PathVariable Long orderId) {
        log.info("[API] Query order status history: orderId={}", orderId);
        return statusHistoryService.listByOrderId(orderId);
    }

    @PostMapping
    public boolean save(@RequestBody OrdStatusHistory history) {
        log.info("[API] Create status history: orderId={}, event={}", history.getOrderId(), history.getEvent());
        return statusHistoryService.save(history);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        log.info("[API] Delete status history: id={}", id);
        return statusHistoryService.removeById(id);
    }
}
