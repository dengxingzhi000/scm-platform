package com.scmcloud.common.lock;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Declarative distributed lock annotation.
 * Wraps the DistributedLock implementation for easy use via AOP.
 *
 * <p>Usage:</p>
 * <pre>
 * {@literal @}DistributedLock(key = "#orderId", ttl = 30, unit = TimeUnit.SECONDS)
 * public void processPayment(Long orderId, PaymentResult result) { ... }
 *
 * {@literal @}DistributedLock(key = "'sku:' + #skuId", ttl = 10)
 * public void deductStock(Long skuId, int quantity) { ... }
 * </pre>
 *
 * @author SCM Platform
 * @since 2026-06-04
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DistributedLockAnnotation {

    /**
     * Lock key. Supports SpEL expressions.
     * Examples: "#orderId", "'sku:' + #skuId", "#tenant.id + ':order:' + #orderNo"
     */
    String key();

    /**
     * Lock time-to-live.
     */
    long ttl() default 30;

    /**
     * Time unit for TTL.
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * Whether to wait for the lock (blocking) or fail immediately.
     */
    boolean blocking() default false;

    /**
     * Wait time if blocking is true.
     */
    long waitTime() default 5;

    /**
     * Error message key if lock acquisition fails.
     */
    String errorMessage() default "lock.acquisition_failed";
}
