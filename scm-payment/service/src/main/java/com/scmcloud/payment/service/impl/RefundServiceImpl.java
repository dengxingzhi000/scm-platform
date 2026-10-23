package com.scmcloud.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.payment.domain.entity.PaymentOrder;
import com.scmcloud.payment.domain.entity.Refund;
import com.scmcloud.payment.mapper.RefundMapper;
import com.scmcloud.payment.service.IPaymentOrderService;
import com.scmcloud.payment.service.IRefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class RefundServiceImpl extends ServiceImpl<RefundMapper, Refund> implements IRefundService {

    private static final int REFUND_PENDING = 1;
    private static final int REFUND_SUCCESS = 2;
    private static final int REFUND_REJECTED = 3;
    private static final int PAYMENT_PAID = 2;

    private final IPaymentOrderService paymentOrderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Refund createRefund(String paymentNo, String orderNo, String userId,
                               BigDecimal amount, String reason) {
        if (paymentNo == null || paymentNo.isBlank()) {
            throw new IllegalArgumentException("paymentNo is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("refund amount must be greater than zero");
        }

        PaymentOrder payment = paymentOrderService.queryPayment(paymentNo);
        if (payment == null) {
            throw new IllegalArgumentException("Payment not found: " + paymentNo);
        }
        if (!payment.getOrderNo().equals(orderNo)) {
            throw new IllegalArgumentException("Payment does not belong to order: " + orderNo);
        }
        if (!payment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Payment does not belong to user: " + userId);
        }
        if (payment.getStatus() == null || payment.getStatus() != PAYMENT_PAID) {
            throw new IllegalStateException("Only paid payments can be refunded");
        }
        if (amount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds payment amount");
        }
        BigDecimal alreadyRefunded = list(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getPaymentNo, paymentNo)
                .eq(Refund::getStatus, REFUND_SUCCESS))
                .stream()
                .map(Refund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (alreadyRefunded.add(amount).compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds remaining refundable balance");
        }

        LocalDateTime now = LocalDateTime.now();
        Refund refund = new Refund()
                .setRefundNo("RF" + System.currentTimeMillis()
                        + UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .setPaymentNo(paymentNo)
                .setOrderNo(orderNo)
                .setUserId(userId)
                .setRefundAmount(amount)
                .setRefundReason(reason)
                .setStatus(REFUND_PENDING)
                .setCreatedAt(now)
                .setUpdatedAt(now);
        save(refund);
        log.info("Refund created: refundNo={}, paymentNo={}, amount={}",
                refund.getRefundNo(), paymentNo, amount);
        return refund;
    }

    @Override
    public Refund queryRefund(String refundNo) {
        return getOne(new LambdaQueryWrapper<Refund>().eq(Refund::getRefundNo, refundNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Refund approveRefund(String refundNo) {
        Refund refund = requirePending(refundNo);
        LocalDateTime now = LocalDateTime.now();
        refund.setStatus(REFUND_SUCCESS);
        refund.setRefundedAt(now);
        refund.setUpdatedAt(now);
        updateById(refund);
        log.info("Refund approved: refundNo={}", refundNo);
        return refund;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Refund rejectRefund(String refundNo, String reason) {
        Refund refund = requirePending(refundNo);
        refund.setStatus(REFUND_REJECTED);
        refund.setRefundReason(reason == null ? refund.getRefundReason()
                : refund.getRefundReason() + " | rejected: " + reason);
        refund.setUpdatedAt(LocalDateTime.now());
        updateById(refund);
        log.info("Refund rejected: refundNo={}, reason={}", refundNo, reason);
        return refund;
    }

    private Refund requirePending(String refundNo) {
        Refund refund = queryRefund(refundNo);
        if (refund == null) {
            throw new IllegalArgumentException("Refund not found: " + refundNo);
        }
        if (refund.getStatus() == null || refund.getStatus() != REFUND_PENDING) {
            throw new IllegalStateException("Refund is not pending: " + refundNo);
        }
        return refund;
    }
}
