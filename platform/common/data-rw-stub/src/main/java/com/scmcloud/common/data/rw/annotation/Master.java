package com.scmcloud.common.data.rw.annotation;

import java.lang.annotation.*;

/**
 * Force routing to master datasource.
 * <p>
 * In the open core edition, this annotation is a no-op — all queries
 * go to the single datasource. In the enterprise edition, the
 * {@code scm-common-data-rw} module provides AOP-based routing.
 *
 * @author Deng
 * @since 2025-12-16
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Master {

    /**
     * Reason description (for logging and monitoring).
     */
    String reason() default "";
}
