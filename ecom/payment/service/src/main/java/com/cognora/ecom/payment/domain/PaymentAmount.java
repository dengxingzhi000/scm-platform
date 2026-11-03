package com.cognora.ecom.payment.domain;

import java.math.BigDecimal;

/**
 * Amount rules shared by the payment service and the validation of its request
 * bodies.
 *
 * <p>Alipay and WeChat Pay settle in cents, so every amount this service stores
 * or compares carries exactly two decimal places.</p>
 */
public final class PaymentAmount {

    /**
     * Decimal places a channel settles in. An amount with more decimals cannot
     * be paid, so it is rejected instead of being rounded.
     */
    public static final int SCALE = 2;

    /**
     * Smallest amount a channel can settle, in yuan.
     */
    public static final String MIN_AMOUNT = "0.01";

    /**
     * Upper bound of a single payment, in yuan. This is the platform's own
     * ceiling for one payment order; it sits far above the per-transaction
     * limit of the channels themselves, so it only rejects values that could
     * never be settled.
     */
    public static final String MAX_AMOUNT = "99999999.99";

    /**
     * {@link #MAX_AMOUNT} as a number, for comparisons.
     */
    public static final BigDecimal MAX_AMOUNT_VALUE = new BigDecimal(MAX_AMOUNT);

    private PaymentAmount() {
    }
}
