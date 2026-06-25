package com.scmcloud.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.payment.domain.entity.PaymentOrder;

import java.math.BigDecimal;

public interface IPaymentOrderService extends IService<PaymentOrder> {

    PaymentOrder createPayment(String orderNo, String userId, BigDecimal amount, String paymentChannel);

    PaymentOrder queryPayment(String paymentNo);

    void cancelPayment(String paymentNo);

    void handlePaymentCallback(String paymentNo, String transactionId);
}
