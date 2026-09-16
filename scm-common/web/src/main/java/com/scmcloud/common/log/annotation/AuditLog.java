package com.scmcloud.common.log.annotation;

import java.lang.annotation.*;

/**
 * 瀹¤鏃ュ織娉ㄨВ
 *
 * @author Deng
 * createData 2025/10/14 17:28
 * @version 1.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 鎿嶄綔鎻忚堪
     */
    String operation() default "";

    /**
     * 涓氬姟绫诲瀷
     */
    String businessType() default "";

    /**
     * 椋庨櫓绛夌骇: 1-锟?2-锟?3-锟?4-鏋侀珮
     */
    int riskLevel() default 1;

    /**
     * 鏄惁璁板綍璇锋眰鍙傛暟
     */
    boolean recordParams() default true;

    /**
     * 鏄惁璁板綍鍝嶅簲缁撴灉
     */
    boolean recordResult() default false;
}
