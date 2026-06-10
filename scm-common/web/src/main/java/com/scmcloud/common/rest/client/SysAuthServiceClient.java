package com.scmcloud.common.rest.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.scmcloud.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.UUID;

/**
 * 璁よ瘉鏈嶅姟瀹㈡埛绔紙@HttpExchange 鐗堟湰锟?
 * <p>鏇夸唬 OpenFeign 锟絊ysAuthServiceClient</p>
 *
 * @author Claude
 * @since 2025-12-29
 */
@HttpExchange("/api/auth")
public interface SysAuthServiceClient {

    /**
     * Logger for security-critical operations
     * <p>Used to audit force logout failures (security events)</p>
     */
    Logger log = LoggerFactory.getLogger(SysAuthServiceClient.class);

    /**
     * 寮哄埗鐢ㄦ埛鐧诲嚭
     *
     * <p>浣跨敤鍦烘櫙锟?
     * <ul>
     *   <li>绠＄悊鍛樺己鍒剁敤鎴蜂笅锟?li>
     *   <li>瀹夊叏绛栫暐瑙﹀彂寮哄埗鐧诲嚭</li>
     *   <li>璐﹀彿寮傚父琛屼负妫€娴嬪悗寮哄埗鐧诲嚭</li>
     * </ul>
     *
     * <p>闄嶇骇绛栫暐锛氳繑锟?03 鏈嶅姟涓嶅彲锟?p>
     *
     * @param userId 鐢ㄦ埛 ID
     * @param reason 鐧诲嚭鍘熷洜
     * @return 鍝嶅簲缁撴灉
     */
    @PostExchange("/force-logout/{userId}")
    @SentinelResource(
        value = "auth-service:forceLogout",
        fallback = "forceLogoutFallback"
    )
    ApiResponse<Void> forceLogout(
        @PathVariable UUID userId,
        @RequestParam("reason") String reason
    );

    /**
     * 寮哄埗鐧诲嚭鐨勯檷绾ф柟锟?
     * <p><strong>SECURITY: 璁板綍瀹夊叏浜嬩欢</strong> - 寮哄埗鐧诲嚭澶辫触鍙兘瀵艰嚧瀹夊叏椋庨櫓</p>
     * <p>闄嶇骇绛栫暐锛氳繑锟?03 鏈嶅姟涓嶅彲锟?p>
     *
     * @param userId 鐢ㄦ埛 ID
     * @param reason 鐧诲嚭鍘熷洜
     * @param ex 寮傚父
     * @return 澶辫触鍝嶅簲
     */
    default ApiResponse<Void> forceLogoutFallback(
        UUID userId,
        String reason,
        Throwable ex) {

        // SECURITY: 璁板綍寮哄埗鐧诲嚭澶辫触锛岃繖鍙兘鏄畨鍏ㄤ簨锟?
        // 渚嬪锛氬彂鐜拌处鍙疯鐩楁兂寮哄埗涓嬬嚎锛屼絾鏈嶅姟涓嶅彲鐢ㄥ鑷存棤娉曠櫥锟?
        log.error("SECURITY ALERT: Force logout failed - potential security risk. " +
                  "userId={}, reason={}, error={}",
                  userId, reason, ex.getMessage());

        // 闄嶇骇杩斿洖澶辫触锛氳璇佹湇鍔′笉鍙敤鏃舵棤娉曞己鍒剁櫥锟?
        return ApiResponse.fail(503, "Auth service temporarily unavailable, please try again later");
    }
}
