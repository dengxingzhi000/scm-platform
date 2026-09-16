package com.scmcloud.gateway.util;

import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

/**
 * 绛惧悕绠楁硶鎺ュ彛
 * <p>
 * 瀹氫箟浜咥PI璇锋眰绛惧悕鐨勮绠楀拰楠岃瘉鏂规硶锛岀敤浜庣'淇濊姹傜殑瀹屾暣鎬у拰鏉ユ簮鍚堟硶鎬э拷
 * 鏀寔鍝嶅簲寮忕紪绋嬫ā鍨嬶紝閫傜敤浜嶴pring WebFlux鐜锟?
 * </p>
 *
 * @author Deng
 * @version 1.0
 * @since 2025/11/11 9:19
 */
public interface SignatureAlgorithm {

    /**
     * 鑾峰彇绛惧悕绠楁硶鐗堟湰锟?
     *
     * @return 绠楁硶鐗堟湰瀛楃锟?
     */
    String version();

    /**
     * 璁$畻璇锋眰绛惧悕
     *
     * @param request   HTTP 璇锋眰瀵硅薄
     * @param appId     搴旂敤鏍囪瘑
     * @param timestamp 鏃堕棿锟?
     * @param nonce     闅忔満锟?
     * @param secretKey 瀵嗛挜
     * @return 璁$畻寰楀嚭鐨勭鍚嶅瓧绗︿覆
     */
    Mono<String> calculate(ServerHttpRequest request, String appId, String timestamp, String nonce, String secretKey);

    /**
     * 楠岃瘉璇锋眰绛惧悕
     *
     * @param request   HTTP 璇锋眰瀵硅薄
     * @param signature 寰呴獙璇佺殑绛惧悕
     * @param appId     搴旂敤鏍囪瘑
     * @param timestamp 鏃堕棿锟?
     * @param nonce     闅忔満锟?
     * @param secretKey 瀵嗛挜
     * @return 绛惧悕楠岃瘉缁撴灉锛宼rue琛ㄧず楠岃瘉閫氳繃锛宖alse琛ㄧず楠岃瘉澶辫触
     */
    Mono<Boolean> verify(ServerHttpRequest request, String signature, String appId, String timestamp, String nonce,
                         String secretKey);
}
