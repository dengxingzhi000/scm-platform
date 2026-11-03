package com.scmcloud.common.data.rw.annotation;

import java.lang.annotation.*;

/**
 * Force routing to slave datasource.
 * <p>
 * In the open core edition, this annotation is a no-op — all queries
 * go to the single datasource. In the enterprise edition, the
 * {@code scm-common-data-rw} module provides AOP-based routing with
 * load balancing across multiple slaves.
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
