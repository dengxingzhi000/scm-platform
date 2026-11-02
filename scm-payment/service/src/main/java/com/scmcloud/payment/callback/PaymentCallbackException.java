package com.scmcloud.payment.callback;

/**
 * A callback failed authentication or did not match the local payment.
 * The message is safe to log; it never contains a key, certificate or raw body.
 */
public class PaymentCallbackException extends RuntimeException {

    public PaymentCallbackException(String message) {
        super(message);
    }

    public PaymentCallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
