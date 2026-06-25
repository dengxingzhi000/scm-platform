package com.scmcloud.common.data.rw.annotation;

import java.lang.annotation.*;

/**
 * Force routing to master datasource.
 * <p>
 * Used for scenarios that need to read latest data, such as:
 * - Read immediately after write
 * - Critical business queries
 * - Read operations in transactions
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
