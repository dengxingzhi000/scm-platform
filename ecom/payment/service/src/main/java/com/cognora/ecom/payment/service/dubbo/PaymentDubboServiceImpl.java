package com.cognora.ecom.payment.service.dubbo;

import com.cognora.ecom.payment.api.PaymentDubboService;
import com.cognora.ecom.payment.domain.entity.PaymentOrder;
import com.cognora.ecom.payment.domain.entity.Refund;
import com.cognora.ecom.payment.service.IPaymentOrderService;
import com.cognora.ecom.payment.service.IRefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;

/**
 * RPC entry point of the payment service.
 *
 * <p>It exposes no operation that marks a payment paid. A payment is completed
 * only by a verified channel callback, which is reached over HTTP - the RPC
 * path has no gateway in front of it and must not offer a shortcut around the
 * signature, channel and amount checks.</p>
 */
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
