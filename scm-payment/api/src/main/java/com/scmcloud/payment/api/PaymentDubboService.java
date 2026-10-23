package com.scmcloud.payment.api;

import java.math.BigDecimal;

public interface PaymentDubboService {

    String createPayment(String orderNo, String userId, BigDecimal amount, String paymentChannel);

    boolean handlePaymentCallback(String paymentNo, String transactionId);

    String createRefund(String paymentNo, String orderNo, String userId, BigDecimal amount, String reason);

    boolean approveRefund(String refundNo);
}
