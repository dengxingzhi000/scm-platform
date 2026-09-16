package com.scmcloud.common.tenant.quota;

import java.lang.annotation.*;

/**
 * 閰嶉妫€鏌ユ敞锟?

 * 浣跨敤绀轰緥锟?
 * <pre>
 * @RequireQuotaCheck(quotaType = QuotaType.ORDERS, increment = 1)
 * public Order createOrder(OrderCreateDTO dto) {
 *     // 鍦ㄦ柟娉曟墽琛屽墠浼氳嚜鍔ㄦ鏌ョ鎴风殑璁㈠崟閰嶉
 *     // 濡傛灉閰嶉涓嶈冻锛屾姏锟絈uotaExceededException
 * }
 * </pre>
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireQuotaCheck {

    /**
     * 閰嶉绫诲瀷
     */
    QuotaType quotaType();

    /**
     * 娑堣€楃殑閰嶉鏁伴噺锛堥粯锟斤拷
     */
    int increment() default 1;

    /**
     * 鏄惁鍦ㄦ柟娉曟垚鍔熷悗鎵嶆秷鑰楅厤棰濓紙榛樿false锛屽嵆鏂规硶鎵ц鍓嶅氨娑堣€楋級
     * 濡傛灉涓簍rue锛岄渶瑕侀厤锟紷AfterReturning 瀹炵幇
     */
    boolean consumeAfterSuccess() default false;
}