package com.scmcloud.common.security.idempotent;

import java.lang.annotation.*;

/**
 * 骞傜瓑鎬ф敞锟?
 * 鐢ㄤ簬闃叉鎺ュ彛閲嶅鎻愪氦
 *
 * @author Deng
 * createData 2025/10/31 10:19
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 骞傜瓑锟絢ey鐨勫墠缂€
     */
    String prefix() default "idempotent:";

    /**
     * 骞傜瓑鎬ey鐨凷pEL琛ㄨ揪锟?
     * 渚嬪: #userId 锟?request.orderId
     */
    String key() default "";

    /**
     * 杩囨湡鏃堕棿锛堢锟?
     */
    int expireTime() default 300;

    /**
     * 鎻愮ず淇℃伅
     */
    String message() default "璇峰嬁閲嶅鎻愪氦";

    /**
     * 骞傜瓑鎬х被锟?
     */
    Type type() default Type.TOKEN;

    enum Type {
        /**
         * Token妯″紡锛氬鎴风鍏堣幏鍙杢oken锛屾彁浜ゆ椂楠岃瘉
         */
        TOKEN,

        /**
         * 鍙傛暟妯″紡锛氭牴鎹弬鏁扮敓鎴愬敮涓€key
         */
        PARAM,

        /**
         * 璇锋眰璺緞妯″紡锛氭牴鎹姹傝矾锟界敤鎴稩D
         */
        PATH
    }
}