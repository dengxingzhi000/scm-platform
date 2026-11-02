package com.scmcloud.payment.callback;

import java.math.BigDecimal;

/**
 * A callback whose channel signature has been verified.
 * {@code paidAmount} is null for non-success notifications.
 */
public record VerifiedPayment(
        String paymentNo,
        String transactionId,
        BigDecimal paidAmount,
        String merchantId,
        boolean success) {
}
