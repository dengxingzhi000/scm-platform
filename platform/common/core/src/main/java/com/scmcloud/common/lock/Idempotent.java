package com.scmcloud.common.lock;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Declarative idempotency annotation.
 * Prevents duplicate execution of the same operation within a TTL window.
 *
 * <p>Usage:</p>
 * <pre>
 * {@literal @}Idempotent(key = "#request.requestId", ttl = 300, unit = TimeUnit.SECONDS)
 * public OrderCreateResult createOrder(CreateOrderRequest request) { ... }
 *
 * {@literal @}Idempotent(key = "'deduct:' + #skuId + ':' + #requestId", ttl = 60)
 * public StockDeductResult deductStock(Long skuId, int qty, String requestId) { ... }
 * </pre>
 *
 * @author SCM Platform
 * @since 2026-06-04
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Idempotent {

    /**
     * Idempotency key. Supports SpEL expressions.
     * The key uniquely identifies the operation.
     */
    String key();

    /**
     * TTL for the idempotency check.
     */
    long ttl() default 300;

    /**
     * Time unit for TTL.
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * Error message if duplicate request is detected.
     * Defaults to the i18n key matching {@code ErrorCode.IDEMPOTENT_REPLAY}.
     */
    String errorMessage() default "idempotent.replay";
}
