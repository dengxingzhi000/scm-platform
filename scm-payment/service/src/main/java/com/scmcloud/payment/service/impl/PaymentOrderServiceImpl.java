package com.scmcloud.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.payment.callback.PaymentCallbackException;
import com.scmcloud.payment.domain.entity.PaymentOrder;
import com.scmcloud.payment.mapper.PaymentOrderMapper;
import com.scmcloud.payment.service.IPaymentOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements IPaymentOrderService {

    private static final int STATUS_PENDING = 1;
    private static final int STATUS_PAID = 2;
    private static final int STATUS_CANCELLED = 4;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder createPayment(String orderNo, String userId, BigDecimal amount, String paymentChannel) {
        log.info("Creating payment: orderNo={}, userId={}, amount={}, channel={}",
                (orderNo + "").replace("\r", "_").replace("\n", "_"),
                (userId + "").replace("\r", "_").replace("\n", "_"),
                (amount + "").replace("\r", "_").replace("\n", "_"),
                (paymentChannel + "").replace("\r", "_").replace("\n", "_"));

        if (!StringUtils.hasText(orderNo)) {
            throw new IllegalArgumentException("orderNo is required");
        }
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("payment amount must be greater than zero");
        }
        if (!StringUtils.hasText(paymentChannel)) {
            throw new IllegalArgumentException("paymentChannel is required");
        }

        // Idempotency: the same order + channel must not create a second payable payment.
        PaymentOrder existing = getOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOrderNo, orderNo)
                .eq(PaymentOrder::getPaymentChannel, paymentChannel)
                .in(PaymentOrder::getStatus, STATUS_PENDING, STATUS_PAID)
                .orderByDesc(PaymentOrder::getCreatedAt)
                .last("LIMIT 1"));
        if (existing != null) {
            if (existing.getStatus() == STATUS_PAID) {
                throw new IllegalStateException("Order already paid: " + orderNo);
            }
            if (existing.getAmount() != null && existing.getAmount().compareTo(amount) != 0) {
                throw new IllegalStateException("Pending payment amount mismatch, existing="
                        + existing.getAmount() + ", requested=" + amount);
            }
            log.info("Reusing pending payment: paymentNo={}, orderNo={}", existing.getPaymentNo(), orderNo);
            return existing;
        }

        PaymentOrder payment = new PaymentOrder();
        payment.setPaymentNo("PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8));
        payment.setOrderNo(orderNo);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setCurrency("CNY");
        payment.setPaymentChannel(paymentChannel);
        payment.setStatus(STATUS_PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        save(payment);
        log.info("Payment created: paymentNo={}", payment.getPaymentNo());
        return payment;
    }

    @Override
    public PaymentOrder queryPayment(String paymentNo) {
        return getOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getPaymentNo, paymentNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPayment(String paymentNo) {
        PaymentOrder payment = queryPayment(paymentNo);
        if (payment == null) {
            throw new IllegalArgumentException("Payment not found: " + paymentNo);
        }

        if (payment.getStatus() != STATUS_PENDING) {
            throw new IllegalStateException("Payment cannot be cancelled, status: " + payment.getStatus());
        }

        payment.setStatus(STATUS_CANCELLED);
        payment.setUpdatedAt(LocalDateTime.now());
        updateById(payment);
        log.info("Payment cancelled: paymentNo={}", (paymentNo + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentCallback(String paymentNo, String transactionId) {
        PaymentOrder payment = requirePayment(paymentNo);
        applyPaid(payment, transactionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleVerifiedPaymentCallback(String paymentNo, String transactionId,
                                              BigDecimal paidAmount, String paymentChannel) {
        PaymentOrder payment = requirePayment(paymentNo);
        if (!StringUtils.hasText(paymentChannel)
                || !paymentChannel.equalsIgnoreCase(payment.getPaymentChannel())) {
            throw new PaymentCallbackException("callback payment channel does not match the local payment");
        }
        if (paidAmount == null || payment.getAmount() == null
                || paidAmount.compareTo(payment.getAmount()) != 0) {
            throw new PaymentCallbackException("callback amount does not match the local payment amount");
        }
        applyPaid(payment, transactionId);
    }

    private PaymentOrder requirePayment(String paymentNo) {
        if (!StringUtils.hasText(paymentNo)) {
            throw new IllegalArgumentException("paymentNo is required");
        }
        PaymentOrder payment = queryPayment(paymentNo);
        if (payment == null) {
            throw new IllegalArgumentException("Payment not found: " + paymentNo);
        }
        return payment;
    }

    private void applyPaid(PaymentOrder payment, String transactionId) {
        if (!StringUtils.hasText(transactionId)) {
            throw new PaymentCallbackException("callback has no transaction id");
        }
        if (payment.getStatus() != null && payment.getStatus() == STATUS_PAID) {
            // Payment gateways retry callbacks; an already-paid order is a no-op.
            log.info("Ignoring duplicate payment callback: paymentNo={}, transactionId={}",
                    payment.getPaymentNo(), transactionId);
            return;
        }
        if (payment.getStatus() == null || payment.getStatus() != STATUS_PENDING) {
            throw new IllegalStateException("Payment is not payable, status: " + payment.getStatus());
        }

        payment.setStatus(STATUS_PAID);
        payment.setTransactionId(transactionId);
        payment.setPaidAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        updateById(payment);
        log.info("Payment callback handled: paymentNo={}, transactionId={}",
                (paymentNo + "").replace("\r", "_").replace("\n", "_"),
                (transactionId + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    public Page<PaymentOrder> pageQuery(int page, int size, String orderNo, String userId,
                                        String paymentChannel, Integer status) {
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(orderNo), PaymentOrder::getOrderNo, orderNo)
                .eq(StringUtils.hasText(userId), PaymentOrder::getUserId, userId)
                .eq(StringUtils.hasText(paymentChannel), PaymentOrder::getPaymentChannel, paymentChannel)
                .eq(status != null, PaymentOrder::getStatus, status)
                .orderByDesc(PaymentOrder::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }
}
