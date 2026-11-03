package com.scmcloud.common.data.rw.annotation;

import java.lang.annotation.*;

/**
 * Force routing to slave datasource.
 * <p>
 * Used for queries that can tolerate latency, such as:
 * - Report statistics
 * - Batch export
 * - Non-realtime queries
 *
 * @author Deng
 * @since 2025-12-16
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Slave {

    /**
     * Slave datasource name (optional, defaults to load balancer selection).
     */
    String value() default "";

    /**
     * Whether to fallback to master when slave is unavailable.
     */
    boolean fallbackToMaster() default true;
}
