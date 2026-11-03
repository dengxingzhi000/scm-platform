package com.scmcloud.payment.service.dubbo;

import com.scmcloud.payment.api.PaymentDubboService;
import com.scmcloud.payment.domain.entity.PaymentOrder;
import com.scmcloud.payment.domain.entity.Refund;
import com.scmcloud.payment.service.IPaymentOrderService;
import com.scmcloud.payment.service.IRefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;

@Slf4j
@DubboService
@RequiredArgsConstructor
public class PaymentDubboServiceImpl implements PaymentDubboService {

    private final IPaymentOrderService paymentOrderService;
    private final IRefundService refundService;

    @Override
    public String createPayment(String orderNo, String userId, BigDecimal amount, String paymentChannel) {
        PaymentOrder payment = paymentOrderService.createPayment(orderNo, userId, amount, paymentChannel);
        return payment.getPaymentNo();
    }

    @Override
    public boolean handlePaymentCallback(String paymentNo, String transactionId) {
        paymentOrderService.handlePaymentCallback(paymentNo, transactionId);
        return true;
    }

    @Override
    public String createRefund(String paymentNo, String orderNo, String userId,
                               BigDecimal amount, String reason) {
        Refund refund = refundService.createRefund(paymentNo, orderNo, userId, amount, reason);
        return refund.getRefundNo();
    }

    @Override
    public boolean approveRefund(String refundNo) {
        refundService.approveRefund(refundNo);
        return true;
    }
}
