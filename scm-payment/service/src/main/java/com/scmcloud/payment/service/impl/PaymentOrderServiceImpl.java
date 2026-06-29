package com.scmcloud.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.payment.domain.entity.PaymentOrder;
import com.scmcloud.payment.mapper.PaymentOrderMapper;
import com.scmcloud.payment.service.IPaymentOrderService;
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
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements IPaymentOrderService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder createPayment(String orderNo, String userId, BigDecimal amount, String paymentChannel) {
        log.info("Creating payment: orderNo={}, userId={}, amount={}, channel={}", orderNo, userId, amount, paymentChannel);

        PaymentOrder payment = new PaymentOrder();
        payment.setPaymentNo("PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8));
        payment.setOrderNo(orderNo);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setCurrency("CNY");
        payment.setPaymentChannel(paymentChannel);
        payment.setStatus(1);
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

        if (payment.getStatus() != 1) {
            throw new IllegalStateException("Payment cannot be cancelled, status: " + payment.getStatus());
        }

        payment.setStatus(4);
        payment.setUpdatedAt(LocalDateTime.now());
        updateById(payment);
        log.info("Payment cancelled: paymentNo={}", paymentNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentCallback(String paymentNo, String transactionId) {
        PaymentOrder payment = queryPayment(paymentNo);
        if (payment == null) {
            throw new IllegalArgumentException("Payment not found: " + paymentNo);
        }

        payment.setStatus(2);
        payment.setPaidAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        updateById(payment);
        log.info("Payment callback handled: paymentNo={}, transactionId={}", paymentNo, transactionId);
    }
}
