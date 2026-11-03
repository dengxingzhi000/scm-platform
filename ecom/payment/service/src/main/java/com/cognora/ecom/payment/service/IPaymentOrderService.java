package com.cognora.ecom.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.ecom.payment.domain.entity.PaymentOrder;

import java.math.BigDecimal;

public interface IPaymentOrderService extends IService<PaymentOrder> {

    PaymentOrder createPayment(String orderNo, String userId, BigDecimal amount, String paymentChannel);

    PaymentOrder queryPayment(String paymentNo);

    void cancelPayment(String paymentNo);

    void handlePaymentCallback(String paymentNo, String transactionId);

    /**
     * Applies a channel callback after its signature was verified, additionally
     * checking that the channel and paid amount match the local payment order.
     */
    void handleVerifiedPaymentCallback(String paymentNo, String transactionId,
                                       BigDecimal paidAmount, String paymentChannel);

    Page<PaymentOrder> pageQuery(int page, int size, String orderNo, String userId,
                                 String paymentChannel, Integer status);
}
