package scm.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.order.domain.entity.OrdRefund;
import scm.order.service.IOrdRefundService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/refunds")
@Tag(name = "退款管理", description = "退款记录CRUD接口")
public class OrdRefundController {

    @Autowired
    private IOrdRefundService refundService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询退款记录")
    public OrdRefund getById(@PathVariable String id) {
        log.info("[API] 查询退款记录: id={}", id);
        return refundService.getById(id);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "查询订单的所有退款记录")
    public List<OrdRefund> listByOrderId(@PathVariable Long orderId) {
        log.info("[API] 查询订单退款记录: orderId={}", orderId);
        return refundService.listByOrderId(orderId);
    }

    @PostMapping
    @Operation(summary = "创建退款记录")
    public OrdRefund createRefund(@RequestBody OrdRefund refund) {
        log.info("[API] 创建退款记录: orderNo={}, refundAmount={}", refund.getOrderNo(), refund.getRefundAmount());
        return refundService.createRefund(refund);
    }

    @PutMapping
    @Operation(summary = "更新退款记录")
    public boolean update(@RequestBody OrdRefund refund) {
        log.info("[API] 更新退款记录: id={}", refund.getId());
        return refundService.updateById(refund);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除退款记录")
    public boolean delete(@PathVariable String id) {
        log.info("[API] 删除退款记录: id={}", id);
        return refundService.removeById(id);
    }
}
