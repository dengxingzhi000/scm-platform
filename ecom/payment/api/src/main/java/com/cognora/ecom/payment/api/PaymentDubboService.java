package com.cognora.ecom.payment.api;

import java.math.BigDecimal;

/**
 * Payment RPC contract.
 *
 * <p>It deliberately exposes no way to mark a payment as paid. A payment is only
 * completed by a channel callback whose signature, channel and amount have been
 * verified. A method that took a payment number plus a transaction id would let
 * any RPC caller mark a payment paid without a channel proof, and the RPC path
 * has no gateway in front of it.</p>
 */
public interface PaymentDubboService {

    String createPayment(String orderNo, String userId, BigDecimal amount, String paymentChannel);

    String createRefund(String paymentNo, String orderNo, String userId, BigDecimal amount, String reason);

    boolean approveRefund(String refundNo);
}
