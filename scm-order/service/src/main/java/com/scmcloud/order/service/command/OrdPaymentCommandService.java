package com.scmcloud.order.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.order.domain.entity.OrdPayment;
import com.scmcloud.order.domain.entity.PaymentStatus;
import com.scmcloud.order.mapper.OrdPaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付命令服务（{@code @Master} + 事务）。
 *
 * <p>仅暴露业务方法（{@code createPayment} / {@code updatePaymentStatus}），
 * 不提供任意字段更新入口（避免绕过 {@link PaymentStatus} 状态校验）。</p>
 *
 * @author SCM Platform Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrdPaymentCommandService {

    private final OrdPaymentMapper ordPaymentMapper;

    @Master(reason = "创建支付记录")
    @Transactional(rollbackFor = Exception.class)
    public OrdPayment createPayment(OrdPayment payment) {
        log.info("创建支付记录: paymentNo={}, orderId={}", payment.getPaymentNo(), payment.getOrderId());

        payment.setStatusEnum(PaymentStatus.PENDING);
        payment.setInitiatedAt(LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();
        payment.setCreateTime(now);
        payment.setUpdateTime(now);

        if (payment.getPaymentAmount() == null) {
            throw new IllegalArgumentException("支付金额不能为空");
        }

        int saved = ordPaymentMapper.insert(payment);
        if (saved <= 0) {
            throw new RuntimeException("创建支付记录失败");
        }
        return payment;
    }

    @Master(reason = "更新支付状态")
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePaymentStatus(UUID paymentId, PaymentStatus target, String reason) {
        log.info("更新支付状态: paymentId={}, target={}", paymentId, target);

        OrdPayment payment = ordPaymentMapper.selectById(paymentId);
        if (payment == null) {
            log.warn("支付记录不存在: paymentId={}", paymentId);
            return false;
        }

        PaymentStatus current = payment.getStatusEnum();
        if (current == null || current.isTerminal()) {
            throw new IllegalStateException("支付已处于终态: " + current);
        }

        payment.setStatusEnum(target);
        payment.setUpdateTime(LocalDateTime.now());
        switch (target) {
            case SUCCESS -> payment.setPaidAt(LocalDateTime.now());
            case FAILED -> payment.setFailedAt(LocalDateTime.now());
            case REFUNDED -> payment.setRefundedAt(LocalDateTime.now());
            default -> { /* 其他状态不需要时间戳 */ }
        }

        int updated = ordPaymentMapper.updateById(payment);
        return updated > 0;
    }
}
