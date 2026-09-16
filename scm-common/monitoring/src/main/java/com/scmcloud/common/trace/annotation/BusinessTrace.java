package com.scmcloud.common.trace.annotation;

import java.lang.annotation.*;

/**
 * 涓氬姟杩借釜娉ㄨВ
 *
 * @author Deng
 * createData 2025/10/21 16:27
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface BusinessTrace {
    /**
     * 鎿嶄綔鍚嶇О
     */
    String operationName() default "";

    /**
     * 鏄惁璁板綍鍙傛暟
     */
    boolean recordArgs() default true;

    /**
     * 鏄惁璁板綍杩斿洖锟?
     */
    boolean recordResult() default false;
}
