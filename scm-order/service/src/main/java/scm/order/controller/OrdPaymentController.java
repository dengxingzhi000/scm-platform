package scm.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.order.domain.entity.OrdPayment;
import scm.order.service.IOrdPaymentService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "支付管理", description = "支付记录CRUD接口")
public class OrdPaymentController {

    @Autowired
    private IOrdPaymentService paymentService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询支付记录")
    public OrdPayment getById(@PathVariable String id) {
        log.info("[API] 查询支付记录: id={}", id);
        return paymentService.getById(id);
    }

    @GetMapping
    @Operation(summary = "查询所有支付记录")
    public List<OrdPayment> list() {
        log.info("[API] 查询所有支付记录");
        return paymentService.list();
    }

    @PostMapping
    @Operation(summary = "创建支付记录")
    public OrdPayment createPayment(@RequestBody OrdPayment payment) {
        log.info("[API] 创建支付记录: orderNo={}, amount={}", payment.getOrderNo(), payment.getPaymentAmount());
        return paymentService.createPayment(payment);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新支付状态")
    public boolean updateStatus(
            @PathVariable Long id,
            @Parameter(description = "支付状态:0-待支付,1-处理中,2-成功,3-失败,4-退款中,5-已退款,6-已取消") @RequestParam Integer status) {
        log.info("[API] 更新支付状态: id={}, status={}", id, status);
        return paymentService.updatePaymentStatus(id, status);
    }

    @PutMapping
    @Operation(summary = "更新支付记录")
    public boolean update(@RequestBody OrdPayment payment) {
        log.info("[API] 更新支付记录: id={}", payment.getId());
        return paymentService.updateById(payment);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除支付记录")
    public boolean delete(@PathVariable String id) {
        log.info("[API] 删除支付记录: id={}", id);
        return paymentService.removeById(id);
    }
}
