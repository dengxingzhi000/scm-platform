package com.scmcloud.common.sentinel.annotation;

import java.lang.annotation.*;

/**
 * 鎺ュ彛闄愭祦娉ㄨВ
 *
 * @author Deng
 * createData 2025/10/20 11:00
 * @version 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 璧勬簮鍚嶇О锛堥粯璁や娇鐢ㄦ柟娉曠鍚嶏級
     */
    String value() default "";

    /**
     * QPS 闃堬拷
     */
    int qps() default 100;

    /**
     * 闄愭祦绫诲瀷锟?QPS 2-绾跨▼锟?
     */
    int grade() default 1;

    /**
     * 鏄惁寮€鍚泦缇ら檺锟?
     */
    boolean clusterMode() default false;
}
